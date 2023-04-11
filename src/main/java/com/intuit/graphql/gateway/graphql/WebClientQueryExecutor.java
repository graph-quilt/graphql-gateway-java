package com.intuit.graphql.gateway.graphql;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.intuit.graphql.gateway.metrics.DownstreamCallEvent;
import com.intuit.graphql.gateway.metrics.ExecutionMetrics.ExecutionMetricsData;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import com.intuit.graphql.gateway.webclient.RequestType;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.webfilter.RequestLoggingExchangeFilter;
import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.AstPrinter;
import graphql.language.Document;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
public class WebClientQueryExecutor implements QueryExecutor {

  private final WebClient webClient;
  private final ServiceDefinition serviceDefinition;

  public WebClientQueryExecutor(final WebClient webClient,
      final ServiceDefinition serviceDefinition) {

    if (StringUtils.isBlank(serviceDefinition.getEndpoint())) {
      throw new ServiceRegistrationException(
          String.format("Remote service must have an endpoint. NAMESPACE=%s",
              serviceDefinition.getNamespace())
      );
    }

    this.webClient = webClient;
    this.serviceDefinition = serviceDefinition;
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput,
      final GraphQLContext context) {

    if (log.isDebugEnabled() && executionInput.getRoot() instanceof Document) {
      log.debug(AstPrinter.printAstCompact((Document) executionInput.getRoot()));
    }

    return Mono.defer(() -> executeDataRequest(executionInput, serviceDefinition, context))
        .subscriberContext(context.getOrDefault(Context.class, Context.empty()))
        .flatMap(response -> response.statusCode().isError()
            ? Mono.error(new DataRetrieverException(response.statusCode().getReasonPhrase()))
            : response.bodyToMono(GraphQLRequest.SPECIFICATION_TYPE_REFERENCE))
        .toFuture();
  }

  private Mono<ClientResponse> executeDataRequest(final ExecutionInput executionInput,
      final ServiceDefinition serviceDefinition, final GraphQLContext graphQLContext) {

    Map<String, Object> bodyMap = new HashMap<>();
    bodyMap.put("query", executionInput.getQuery());
    bodyMap.put("variables", executionInput.getVariables());

    String graphqlQueryStr;
    try {
      graphqlQueryStr = Mapper.mapper().writeValueAsString(bodyMap);
    } catch (JsonProcessingException e) {
      throw new DataRetrieverException("Failed to craft downstream query request for " +
          this.serviceDefinition.getEndpoint(), e);
    }

    Map<String, List<String>> headers = new HashMap<>();
    Optional<ServerRequest> serverRequest = graphQLContext.getOrEmpty(ServerRequest.class);

    serverRequest.ifPresent(sr -> serviceDefinition.getForwardHeaders()
        .forEach(key -> headers.computeIfAbsent(key, sr.headers().asHttpHeaders()::get)));

    Map<String, Object> hints = new HashMap<>(2);
    hints.put("requestType", RequestType.QUERY);
    hints.put("namespace", serviceDefinition.getNamespace());

    DownstreamCallEvent downstreamCallEvent = new DownstreamCallEvent(serviceDefinition.getNamespace(),
        serviceDefinition.getAppId());

    return webClient
        .post()
        .uri(this.serviceDefinition.getEndpoint())
        .attribute(RequestLoggingExchangeFilter.REQUEST_LOGGING_HINTS, hints)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .headers(httpHeaders -> httpHeaders.putAll(headers))
        .syncBody(graphqlQueryStr)
        .exchange()
        .timeout(Duration.ofMillis(serviceDefinition.getTimeout()))
        .doOnSubscribe(subscription -> downstreamCallEvent.getEventStopWatch().start())
        .doOnError(throwable -> {
          // .timeout() should throw java.util.concurrent.TimeoutException per projectreactor documentation
          if (throwable instanceof TimeoutException) {
            completeDownstreamCallEvent(downstreamCallEvent, graphQLContext);
          }
        })
        .doFinally(signalType -> {
          completeDownstreamCallEvent(downstreamCallEvent, graphQLContext);
        });
  }

  private void completeDownstreamCallEvent(DownstreamCallEvent downstreamCallEvent, GraphQLContext graphQLContext) {
    downstreamCallEvent.getEventStopWatch().stop();
    ExecutionMetricsData executionMetricsData = graphQLContext.get(ExecutionMetricsData.class);
    if (Objects.nonNull(executionMetricsData)) {
      executionMetricsData.addDownstreamCallEvent(downstreamCallEvent);
    }
  }
}
