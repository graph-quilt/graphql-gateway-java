package com.intuit.graphql.gateway.provider;

import static com.intuit.graphql.gateway.provider.ServiceProviderHelper.validate;

import com.intuit.graphql.gateway.graphql.WebClientQueryExecutor;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.batch.QueryExecutor;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Loads Graphql schema via graphql sdl files
 */
@Slf4j
public class SdlServiceProvider implements ServiceProvider {

  private final SdlServiceRegistration sdlServiceRegistration;
  private final QueryExecutor queryFunction;

  public SdlServiceProvider(final SdlServiceRegistration sdlServiceRegistration, WebClient webClient) {
    this.sdlServiceRegistration = sdlServiceRegistration;
    this.queryFunction = new WebClientQueryExecutor(webClient, sdlServiceRegistration.getServiceDefinition());
  }

  @Override
  public String getNameSpace() {
    return sdlServiceRegistration.getServiceDefinition().getNamespace();
  }

  @Override
  public Map<String, String> sdlFiles() {
    return sdlServiceRegistration.getGraphqlResources();
  }

  @Override
  public Set<String> domainTypes() {
    return sdlServiceRegistration.getServiceDefinition().getDomainTypes();
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput,
      final GraphQLContext context) {
    validate(sdlServiceRegistration.getServiceDefinition(), context);
    return queryFunction.query(executionInput, context);
  }
}
