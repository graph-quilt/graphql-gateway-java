package com.intuit.graphql.gateway.integration;

import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.graphql.GraphQLExecutor;
import com.intuit.graphql.gateway.provider.SdlServiceProvider;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.orchestrator.GraphQLOrchestrator;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.web.reactive.function.client.WebClient;

public class GraphQLExecutorRule implements TestRule {

  private GraphQLExecutor<ExecutionInput> graphqlExecutor;

  public CompletableFuture<ExecutionResult> execute(ExecutionInput executionInput) {
    if (graphqlExecutor == null) {
      graphqlExecutor = buildGraphQLExecutor();
    }
    return graphqlExecutor.execute(executionInput);
  }

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

  private GraphQLExecutor<ExecutionInput> buildGraphQLExecutor() {
    return new GraphQLExecutor<ExecutionInput>() {

      private RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder().services(defaultServices()).build()
          .stitchGraph();
      private GraphQLOrchestrator graphQLOrchestrator = GraphQLOrchestrator.newOrchestrator().runtimeGraph(runtimeGraph)
          .build();

      @Override
      public CompletableFuture<ExecutionResult> execute(final ExecutionInput executionInput) {
        return graphQLOrchestrator.execute(executionInput);
      }
    };
  }

  private static final WebClient webClient = WebClient.create();

  private List<ServiceProvider> defaultServices() {

    Map<String, String> baseTypeProviderSdlFiles = new HashMap<>();
    baseTypeProviderSdlFiles.put("baseTypesProvider", getSdlFile("baseTypesProvider.graphql"));

    final ServiceDefinition serviceDefinition = ServiceDefinition.newBuilder()
        .endpoint("http://localhost:" + wireMockRule.port() + "/standardtypes")
        .namespace("StandardTypes").type(Type.GRAPHQL).build();
    SdlServiceRegistration sdlServiceRegistration = new SdlServiceRegistration(serviceDefinition,
        baseTypeProviderSdlFiles);

    ServiceProvider standardTypesService = new SdlServiceProvider(sdlServiceRegistration, webClient);

    return Collections.singletonList(standardTypesService);
  }

  private String getSdlFile(String filename) {
    try {
      return Resources.toString(Resources.getResource("integration/schemas/" + filename), Charset.defaultCharset());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return wireMockRule.apply(base, description);
  }
}
