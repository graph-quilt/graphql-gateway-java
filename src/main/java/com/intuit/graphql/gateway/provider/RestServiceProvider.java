package com.intuit.graphql.gateway.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.graphql.adapter.core.ServiceAdapter;
import com.intuit.graphql.adapter.core.ServiceAdapterRequest;
import com.intuit.graphql.adapter.core.ServiceAdapterResponse;
import com.intuit.graphql.adapter.rest.MapBasedServiceConfiguration;
import com.intuit.graphql.adapter.rest.RestAdapter;
import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.config.properties.WebClientProperties;
import com.intuit.graphql.gateway.exception.RestExecutionException;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.metrics.DownstreamCallEvent;
import com.intuit.graphql.gateway.metrics.ExecutionMetrics;
import com.intuit.graphql.gateway.registry.RestServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.webclient.RequestType;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.service.dsl.evaluator.ServiceConfiguration;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.intuit.graphql.gateway.Mapper.mapper;
import static com.intuit.graphql.gateway.provider.ServiceProviderHelper.validate;

@Slf4j
public class RestServiceProvider implements ServiceProvider {


  private static final String HEADERS = "headers";
  private static final String COOKIES = "cookies";
  private static final String ARGUMENTS = "arguments";
  public static final String REQUEST_CONTEXT = "requestContext";
  private static final String TRANSACTION_CONTEXT = "transactionContext";

  private final TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<Map<String, Object>>() {
  };

  private final RestServiceRegistration restServiceRegistration;
  private final ServiceAdapter serviceAdapter;

  public RestServiceProvider(final RestServiceRegistration restServiceRegistration,
      final WebClient webClient) {
    ServiceConfiguration serviceConfiguration = createServiceConfiguration(
        restServiceRegistration.getServiceDefinition());
    this.serviceAdapter = RestAdapter.builder()
        .dslResource(getServiceFlowContent(restServiceRegistration.getFlowResources()))
        .serviceId(restServiceRegistration.getServiceDefinition().getNamespace())
        .svcConfig(serviceConfiguration)
        .webClient(webClient)
        .build();
    this.restServiceRegistration = restServiceRegistration;
  }

  private ServiceConfiguration createServiceConfiguration(ServiceDefinition serviceDefinition) {
    Map<String, String> properties = new HashMap<>();
    properties.put("namespace", serviceDefinition.getNamespace());
    properties.put("endpoint", serviceDefinition.getEndpoint());
    properties.put("appid", serviceDefinition.getAppId());
    properties.put("timeout", Long.toString(serviceDefinition.getTimeout()));
    return new MapBasedServiceConfiguration(properties);
  }

  @Override
  public String getNameSpace() {
    return restServiceRegistration.getServiceDefinition().getNamespace();
  }

  @Override
  public Map<String, String> sdlFiles() {
    return restServiceRegistration.getGraphqlResources();
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    ServiceDefinition serviceDefinition = restServiceRegistration.getServiceDefinition();
    validate(serviceDefinition, context);
    ServiceAdapterRequest request = ServiceAdapterRequest.from(toInputMap(context), context, RequestType.QUERY.name());

    CompletableFuture<ServiceAdapterResponse> futureResponse = serviceAdapter.execute(request);
    DownstreamCallEvent downstreamCallEvent = new DownstreamCallEvent(serviceDefinition.getNamespace(),
        serviceDefinition.getAppId());
    return futureResponse
        .handle((response, throwable) -> {
          if (Objects.nonNull(throwable)) {
            throw new RestExecutionException(throwable.getMessage(), throwable.getCause(), serviceDefinition);
          }
          return mapper().convertValue(response, mapTypeReference);
        })
        .whenComplete((data, throwable) -> {
          completeDownstreamCallEvent(downstreamCallEvent, context);
        });
  }

  private void completeDownstreamCallEvent(DownstreamCallEvent downstreamCallEvent, GraphQLContext graphQLContext) {
    downstreamCallEvent.getEventStopWatch().stop();
    ExecutionMetrics.ExecutionMetricsData executionMetricsData = graphQLContext.get(ExecutionMetrics.ExecutionMetricsData.class);
    executionMetricsData.addDownstreamCallEvent(downstreamCallEvent);
  }

  @Override
  public ServiceType getSeviceType() {
    return ServiceType.REST;
  }

  private String getServiceFlowContent(Map<String, String> dslResources) {
    final StringBuilder dsl = new StringBuilder();
    dslResources.forEach((key, content) -> {
      if (Predicates.isFlowFile.test(key)) {
        dsl.append(content);
      }
    });
    return dsl.toString();
  }

  private static Map<String, JsonNode> toInputMap(GraphQLContext graphQLContext) {
    ServerRequest httpRequest = graphQLContext.get(ServerRequest.class);
    DataFetchingEnvironment dfe = graphQLContext.get(DataFetchingEnvironment.class);
    Context reactorContext = graphQLContext.get(Context.class);
    TransactionContext txnCtx = reactorContext.get(TransactionContext.class);

    ObjectNode requestContextNode = mapper().createObjectNode();
    requestContextNode.replace(TRANSACTION_CONTEXT, mapper()
        .convertValue(txnCtx, JsonNode.class));
    requestContextNode
        .replace(ARGUMENTS, mapper().convertValue(dfe.getArguments(), JsonNode.class));
    requestContextNode.replace(COOKIES, mapper().convertValue(getCookiesFromServerRequest(httpRequest),
        JsonNode.class));
    requestContextNode.replace(HEADERS, mapper().convertValue(httpRequest.headers().asHttpHeaders().toSingleValueMap(),
        JsonNode.class));

    Map<String, JsonNode> inputMap = new HashMap<>();
    inputMap.put(REQUEST_CONTEXT, requestContextNode);
    return inputMap;
  }

  private static Map<String, String> getCookiesFromServerRequest(ServerRequest serverRequest) {
    Map<String, String> cookies = new HashMap<>();
    // CSM uses singleValueMap.
    Map<String, HttpCookie> inCookies = serverRequest.cookies().toSingleValueMap();
    inCookies.keySet().forEach(key -> {
      if (inCookies.get(key).getValue().length() > 0) {
        cookies.put(key, inCookies.get(key).getValue());
      }
    });
    return cookies;
  }

}
