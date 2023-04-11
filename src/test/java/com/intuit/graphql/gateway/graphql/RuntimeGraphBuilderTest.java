package com.intuit.graphql.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.provider.SdlServiceProvider;
import com.intuit.graphql.gateway.provider.ServiceBuilder;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import graphql.schema.GraphQLNamedType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
public class RuntimeGraphBuilderTest {

  private final String SDL_SCHEMA_V4OS;
  private final String SDL_SCHEMA_EPS;
  private final String INVALID_SCHEMA;

  TransactionContext tx = ImmutableTransactionContext.builder().build();

  @Mock
  ServiceBuilder serviceBuilder;

  public RuntimeGraphBuilderTest() throws Exception {

    this.SDL_SCHEMA_V4OS = Resources
        .toString(Resources.getResource("sdls/v4os_small.graphqls"), Charsets.UTF_8);
    this.SDL_SCHEMA_EPS = Resources
        .toString(Resources.getResource("sdls/eps.graphql"), Charsets.UTF_8);
    this.INVALID_SCHEMA = Resources
        .toString(Resources.getResource("sdls/invalid_schema.graphqls"), Charsets.UTF_8);
  }

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void returnsEmptySchemaWithNoServiceDefinitionsTest() {
    RuntimeGraphBuilder builder = new RuntimeGraphBuilder(serviceBuilder, mock(BatchLoaderExecutionHooks.class));
    Mono<RuntimeGraph> runtimeGraphMono = builder.build(Flux.empty())
        .subscriberContext(context -> context.putNonNull(TransactionContext.class, tx));
    RuntimeGraph runtimeGraph = runtimeGraphMono.block();
    assertThat(runtimeGraph.getExecutableSchema().getAllTypesAsList().size()).isEqualTo(12);
  }

  @Test
  public void GraphQLOrchestratorBuilderBuildsServicePerDefinitionTest() {

    ServiceProvider sp1 = Mockito.mock(ServiceProvider.class);
    ServiceProvider sp2 = Mockito.mock(ServiceProvider.class);
    when(sp1.getNameSpace()).thenReturn("1");
    when(sp2.getNameSpace()).thenReturn("2");

    when(serviceBuilder.buildService(any(), any())).thenReturn(sp1, sp1, sp2);

    RuntimeGraphBuilder builder = new RuntimeGraphBuilder(serviceBuilder, mock(BatchLoaderExecutionHooks.class));

    verify(serviceBuilder, times(0)).buildService(any(), any());
    ServiceDefinition serviceDefinition = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();
    builder.build(Flux.just(ServiceRegistration.baseBuilder().serviceDefinition(serviceDefinition).build())).block();
    verify(serviceBuilder, times(1)).buildService(any(), any());

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();
    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("2").type(Type.GRAPHQL).build();

    builder.build(Flux.just(
        ServiceRegistration.baseBuilder().serviceDefinition(sd1).build(),
        ServiceRegistration.baseBuilder().serviceDefinition(sd2).build()
    )).block();
    verify(serviceBuilder, times(3)).buildService(any(), any());
  }


  @Test
  public void shouldTerminateIfRuntimeGraphFailsWhileRegistering() {

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();
    ServiceRegistration registrationPackage1 = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();

    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("test").endpoint("test").type(Type.GRAPHQL_SDL)
        .build();
    Map<String, String> graphqlResources2 = new HashMap<>();
    graphqlResources2.put("test-path", SDL_SCHEMA_EPS);
    ServiceRegistration registrationPackage2 = SdlServiceRegistration.builder().serviceDefinition(sd2)
        .graphqlResources(graphqlResources2).build();

    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage1)))
        .thenThrow(new ServiceRegistrationException("Failed to build service provider"));
    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage2)))
        .thenReturn(new SdlServiceProvider((SdlServiceRegistration) registrationPackage2, null));

    RuntimeGraphBuilder builder = new RuntimeGraphBuilder(serviceBuilder, mock(BatchLoaderExecutionHooks.class));
    Mono<RuntimeGraph> runtimeGraphMono = builder.build(Flux.just(registrationPackage1, registrationPackage2));

    StepVerifier.create(runtimeGraphMono)
            .consumeErrorWith(throwable -> assertThat(throwable).isInstanceOf(ServiceRegistrationException.class))
            .verify();  }

 @Test
  public void shouldContinueRegisteringIfSomeFailedAndRegistrationsAreS3() {

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();
    ServiceRegistration registrationPackage1 = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();

    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("test").endpoint("test").type(Type.GRAPHQL_SDL)
        .build();
    Map<String, String> graphqlResources2 = new HashMap<>();
    graphqlResources2.put("test-path", SDL_SCHEMA_EPS);
    ServiceRegistration registrationPackage2 = SdlServiceRegistration.builder().serviceDefinition(sd2)
        .graphqlResources(graphqlResources2).build();

    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage1)))
        .thenThrow(new ServiceRegistrationException("Failed to build service provider"));
    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage2)))
        .thenReturn(new SdlServiceProvider((SdlServiceRegistration) registrationPackage2, null));

    RuntimeGraphBuilder builder = new RuntimeGraphBuilder(serviceBuilder, mock(BatchLoaderExecutionHooks.class));
    RuntimeGraph runtimeGraph = builder.build(Flux.just(registrationPackage1, registrationPackage2), true).block();

    assertThat(runtimeGraph.getExecutableSchema().getAllTypesAsList().size()).isGreaterThan(12);
  }

  @Test
  public void returnsSchemaWithMultipleServiceDefinitionsStitchedTest() {

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("test1").endpoint("test").type(Type.GRAPHQL_SDL)
        .build();
    Map<String, String> graphqlResources1 = new HashMap<>();
    graphqlResources1.put("test-path", SDL_SCHEMA_V4OS);
    ServiceRegistration registrationPackage1 = SdlServiceRegistration.builder().serviceDefinition(sd1)
        .graphqlResources(graphqlResources1).build();

    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("test2").endpoint("test").type(Type.GRAPHQL_SDL)
        .build();
    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("test-path", SDL_SCHEMA_EPS);
    ServiceRegistration registrationPackage2 = SdlServiceRegistration.builder().serviceDefinition(sd2)
        .graphqlResources(graphqlResources).build();

    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage1)))
        .thenReturn(new SdlServiceProvider((SdlServiceRegistration) registrationPackage1, null));
    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage2)))
        .thenReturn(new SdlServiceProvider((SdlServiceRegistration) registrationPackage2, null));

    RuntimeGraphBuilder builder = new RuntimeGraphBuilder(serviceBuilder, mock(BatchLoaderExecutionHooks.class));
    RuntimeGraph runtimeGraph = builder.build(Flux.just(registrationPackage1, registrationPackage2))
        .block();

    assertThat(runtimeGraph.getExecutableSchema().getAllTypesAsList().size()).isGreaterThan(12);

    assertThat(runtimeGraph.getExecutableSchema()
        .getAllTypesAsList()
        .stream()
        .map(GraphQLNamedType::getName)
        .collect(Collectors.toList())
    ).contains("TaxType", "ProfilePhoto");
  }

  @Test
  public void shouldTerminateOnSchemaStitchErrors() {

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("test").endpoint("test").type(Type.GRAPHQL_SDL)
        .build();
    Map<String, String> graphqlResources1 = new HashMap<>();
    graphqlResources1.put("test-path", SDL_SCHEMA_V4OS);
    ServiceRegistration registrationPackage1 = SdlServiceRegistration.builder().serviceDefinition(sd1)
        .graphqlResources(graphqlResources1).build();

    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("test").endpoint("test").type(Type.GRAPHQL_SDL)
        .build();
    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("test-path", INVALID_SCHEMA);
    ServiceRegistration registrationPackage2 = SdlServiceRegistration.builder().serviceDefinition(sd2)
        .graphqlResources(graphqlResources).build();

    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage1)))
        .thenReturn(new SdlServiceProvider((SdlServiceRegistration) registrationPackage1, null));
    when(serviceBuilder.buildService(any(TransactionContext.class), eq(registrationPackage2)))
        .thenReturn(new SdlServiceProvider((SdlServiceRegistration) registrationPackage2, null));

    Mono<RuntimeGraph> runtimeGraphMono = new RuntimeGraphBuilder(serviceBuilder, mock(BatchLoaderExecutionHooks.class))
        .build(Flux.just(registrationPackage1, registrationPackage2));

    StepVerifier.create(runtimeGraphMono)
        .consumeErrorWith(throwable -> assertThat(throwable).isInstanceOf(Exception.class))
        .verify();
  }

  //TODO: Add tests case for schema build with introspection

}