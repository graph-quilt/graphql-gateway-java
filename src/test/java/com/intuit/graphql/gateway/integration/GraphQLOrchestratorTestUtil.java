package com.intuit.graphql.gateway.integration;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.provider.IntrospectionServiceProvider;
import com.intuit.graphql.gateway.provider.SdlServiceProvider;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.orchestrator.GraphQLOrchestrator;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Document;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.SchemaPrinter.Options;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Configuration
public class GraphQLOrchestratorTestUtil {

  private static final WebClient webClient = WebClient.create();

  public static GraphQLOrchestrator getGraphQLOrchestrator() {

    GraphQLOrchestrator.Builder orchestrator = GraphQLOrchestrator.newOrchestrator();

    ServiceDefinition serviceDefinitionV4OS = ServiceDefinition.newBuilder()
        .namespace("V4OS")
        .endpoint("http://localhost:4040/v4os/graphql")
        .timeout(4000)
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinitionDOS = ServiceDefinition.newBuilder()
        .namespace("DOS")
        .forwardHeaders(new HashSet<>(Arrays.asList("foo", "bar")))
        .timeout(4000)
        .endpoint("http://localhost:4040/dos/graphql")
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinitionCGCS = ServiceDefinition.newBuilder()
        .namespace("CGCS")
        .endpoint("http://localhost:4040/cgcs/graphql")
        .timeout(4000)
        .clientWhitelist(Collections.singleton("test.client"))
        .type(Type.GRAPHQL_SDL).build();

    SdlServiceRegistration serviceRegistrationV4 = SdlServiceRegistration.builder()
        .serviceDefinition(serviceDefinitionV4OS).build();
    SdlServiceRegistration serviceRegistrationDOS = SdlServiceRegistration.builder()
        .serviceDefinition(serviceDefinitionDOS).build();
    SdlServiceRegistration serviceRegistrationCGCS = SdlServiceRegistration.builder()
        .serviceDefinition(serviceDefinitionCGCS).build();

    ServiceProvider v4Service = spy(
        new SdlServiceProvider(serviceRegistrationV4, webClient));
    ServiceProvider dosService = spy(
        new SdlServiceProvider(serviceRegistrationDOS, webClient));
    ServiceProvider cgcsService = spy(
        new SdlServiceProvider(serviceRegistrationCGCS, webClient));

    doReturn(makeSDL("introspections/v4os_medium.json"))
        .when(v4Service).sdlFiles();
    doReturn(makeSDL("introspections/dos.json"))
        .when(dosService).sdlFiles();
    doReturn(ImmutableMap.of("somekey", "type Query { cgcs: String } "))
        .when(cgcsService).sdlFiles();

    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder().service(v4Service).service(dosService).service(cgcsService)
        .build()
        .stitchGraph();
    orchestrator.runtimeGraph(runtimeGraph);
    return orchestrator.build();
  }

  private static Map<String, String> makeSDL(String schemaFile) {
    String introspectedSchema;

    try {
      introspectedSchema = Resources.toString(Resources.getResource(schemaFile), Charsets.UTF_8);

      // parse the schema as a Map
      Map result = new ObjectMapper().readValue(introspectedSchema, Map.class);

      // get the 'data' field of the response, which has the schema in it
      Document schemaDoc = new IntrospectionResultToSchema()
          .createSchemaDefinition((Map<String, Object>) result.get("data"));

      // builds the TypeDefinitionRegistry from the schema document
      String sdl = new SchemaPrinter(Options.defaultOptions()
          .includeScalarTypes(true)
          //.includeExtendedScalarTypes(true)
      ).print(schemaDoc);
      return ImmutableMap.of(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME, sdl);
    } catch (IOException ignored) {
      return null;
    }
  }
}