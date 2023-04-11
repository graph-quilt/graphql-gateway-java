package com.intuit.graphql.gateway.provider;

import static com.intuit.graphql.gateway.provider.ServiceProviderHelper.validate;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.graphql.WebClientQueryExecutor;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import com.intuit.graphql.gateway.utils.IntrospectionResultToSchema;
import com.intuit.graphql.gateway.webclient.RequestType;
import com.intuit.graphql.gateway.webfilter.RequestLoggingExchangeFilter;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import graphql.Directives;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.introspection.IntrospectionQuery;
import graphql.language.Document;
import graphql.schema.GraphQLDirective;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.SchemaPrinter.Options;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


/**
 * Loads Graphql schema via graphql introspection
 */
@Slf4j
public class IntrospectionServiceProvider implements ServiceProvider {

  private static final String INTROSPECTION_QUERY = Mapper.mapper().createObjectNode()
      .set("query", new TextNode(IntrospectionQuery.INTROSPECTION_QUERY)).toString();
  public static final String INTROSPECTION_FILE_NAME = "introspection";


  public static final Predicate<GraphQLDirective> SkipDefaultDirectives = directive -> !(directive.getName().equals(
      Directives.IncludeDirective.getName()) || directive.getName().equals(Directives.SkipDirective.getName()));

  @Value("${webclient.introspection.retries}")
  public static int maxRetryAttempts = 3;

  private final ServiceRegistration serviceRegistration;
  private final WebClient webClient;
  private final TransactionContext tx;
  private final QueryExecutor queryFunction;
  private String cache;

  public IntrospectionServiceProvider(final TransactionContext tx, final WebClient webClient,
      final ServiceRegistration serviceRegistration) {

    this.serviceRegistration = ensureValidServiceDefinition(serviceRegistration);
    this.webClient = webClient;
    this.tx = tx;
    this.queryFunction = new WebClientQueryExecutor(this.webClient, serviceRegistration.getServiceDefinition());
    this.cache = getDocument();
  }

  private ServiceRegistration ensureValidServiceDefinition(ServiceRegistration serviceRegistration) {
    // verify endpoint is there
    final ServiceDefinition serviceDefinition = serviceRegistration.getServiceDefinition();
    if (StringUtils.isBlank(serviceDefinition.getEndpoint())) {
      EventLogger.error(log, tx,
          String.format("Failed to register namespace %s", serviceDefinition.getNamespace())
      );
      throw new ServiceRegistrationException(
          String.format("Service must have remote endpoint. NAMESPACE=%s",
              serviceDefinition.getNamespace())
      );
    }

    return serviceRegistration;
  }

  public ServiceDefinition serviceDefinition() {
    return this.serviceRegistration.getServiceDefinition();
  }

  /**
   * retrieves remote schema as TypeDefinitionRegistry
   *
   * @return TypeDefinitionRegistry instance representing remote schema
   */
  private String getDocument() {
    if (cache == null) {
      try {
        // make remote graphql schema introspection call
        String schemaStr = loadRemoteGraphqlSchema();

        // parse the schema as a Map
        Map result = Mapper.mapper().readValue(schemaStr, Map.class);

        // get the 'data' field of the response, which has the schema in it
        Document document = new IntrospectionResultToSchema()
            .createSchemaDefinition((Map<String, Object>) result.get("data"));
        /*
         * Graphql-java IntrospectionResultToSchema converts descriptions to comments. Using local version of
         * IntrospectionResultToSchema to preserve descriptions.
         */
        cache = new SchemaPrinter(Options.defaultOptions()
            .includeScalarTypes(true)
            .includeDirectives(SkipDefaultDirectives)
            //.includeExtendedScalarTypes(true)
        ).print(document);
      } catch (Exception e) {
        EventLogger.error(log, tx, String.format("Could not load remote schema from %s",
            this.serviceDefinition().getEndpoint()), e
        );
        throw new ServiceRegistrationException("Failed to load schema from remote service.", e);
      }
    }
    return cache;
  }

  /**
   * Make the introspection query to the remote service. NOTE: This is ~~~ BLOCKING ~~~
   *
   * @return Response string from remote introspection query
   */
  private String loadRemoteGraphqlSchema() {

    String namespace = this.serviceDefinition().getNamespace();
    String url = this.serviceDefinition().getEndpoint();

    Map<String, Object> hints = new HashMap<>(2);

    hints.put("requestType", RequestType.INTROSPECTION);
    hints.put("namespace", namespace);

    // Prepare schema introspection query
    return this.webClient.post()
        .uri(url)
        .attribute(RequestLoggingExchangeFilter.REQUEST_LOGGING_HINTS, hints)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .syncBody(INTROSPECTION_QUERY)
        .exchange()
        .flatMap(response -> {
          if (response.statusCode().isError()) {
            return Mono
                .error(new WebClientResponseException(response.rawStatusCode(), response.statusCode().getReasonPhrase(),
                    null, null, null));
          }
          return Mono.just(response);
        })
        .retryWhen(Retry.withThrowable(this.retryFactory()))
        .flatMap(response -> response.bodyToMono(String.class))
        .subscriberContext(context -> context.putNonNull(TransactionContext.class, tx))
        .block();
  }

  private Function<Flux<Throwable>, ? extends Publisher<?>> retryFactory() {
    return eFlux -> eFlux.zipWith(Flux.range(1, maxRetryAttempts + 1), (e, attempts) -> {

      if (e instanceof WebClientResponseException) {
        WebClientResponseException webClientResponseException = (WebClientResponseException) e;
        if (webClientResponseException.getStatusCode().is4xxClientError()) {
          EventLogger.error(log, tx, "Received 4xx error on introspection. Aborting.", e);
          throw Exceptions.propagate(e);
        }
      }

      if (attempts < maxRetryAttempts) {
        EventLogger.warn(log, tx, "Retrying introspection request. Attempts[" + attempts + "]", e);
        return attempts;
      } else {
        EventLogger.error(log, tx, "Max introspection attempts reached. Aborting.", e);
        throw Exceptions.propagate(e);
      }
    });
  }

  @Override
  public String getNameSpace() {
    return serviceDefinition().getNamespace();
  }

  @Override
  public Map<String, String> sdlFiles() {
    return ImmutableMap.of(INTROSPECTION_FILE_NAME, getDocument());
  }

  @Override
  public Set<String> domainTypes() {
    return serviceRegistration.getServiceDefinition().getDomainTypes();
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput,
      final GraphQLContext context) {
    validate(serviceRegistration.getServiceDefinition(), context);
    return queryFunction.query(executionInput, context);
  }
}
