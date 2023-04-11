package com.intuit.graphql.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.config.properties.StitchingProperties;
import com.intuit.graphql.gateway.events.GraphQLSchemaChangedEvent;
import com.intuit.graphql.gateway.registry.S3ServiceRegistrationProvider;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationProvider;
import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class SchemaManagerTest {

  @Mock
  public StitchingProperties properties;

  @Mock
  public ApplicationEventPublisher eventPublisher;

  @Mock
  public RuntimeGraphBuilder mockRuntimeGraphBuilder;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(mockRuntimeGraphBuilder.build(any(), anyBoolean())).thenReturn(Mono.just(SchemaStitcher.newBuilder().build().stitchGraph()));
  }

  @Test
  public void initRetrievesAllProvidersServiceRegistrationsTest() {

    ServiceRegistrationProvider provider1 = mock(S3ServiceRegistrationProvider.class);

    when(provider1.getInitialServiceRegistrations()).thenReturn(Flux.empty());

    RuntimeGraphBuilder builder = new RuntimeGraphBuilder(null, mock(BatchLoaderExecutionHooks.class));

    SchemaManager managerUnderTest = new SchemaManager(TestHelper.testTxProvider(), builder,
        Arrays.asList(provider1), eventPublisher);

    managerUnderTest.initializeRuntimeGraph();

    verify(provider1).getInitialServiceRegistrations();
  }

  @Test
  public void updateRegistryBuildsDistinctServiceRegistrationsTest() {
    ServiceRegistrationProvider provider1 = mock(S3ServiceRegistrationProvider.class);

    SchemaManager managerUnderTest = new SchemaManager(TestHelper.testTxProvider(),
        mockRuntimeGraphBuilder, Arrays.asList(provider1), eventPublisher);

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("test-namespace").type(Type.REST)
        .appId("test-appId")
        .endpoint("endpoint").build();
    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("test-namespace").type(Type.REST)
        .appId("test-appId")
        .endpoint("endpoint").build();

    ServiceRegistration sr1 = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();
    ServiceRegistration sr2 = ServiceRegistration.baseBuilder().serviceDefinition(sd2).build();

    ArgumentCaptor<Flux> fluxArgCaptor = ArgumentCaptor.forClass(Flux.class);
    ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);

    managerUnderTest.updateRegistry(provider1.getClass().getName(), Flux.just(sr1, sr2));
    verify(mockRuntimeGraphBuilder).build(fluxArgCaptor.capture(), booleanArgumentCaptor.capture());
    StepVerifier.create(fluxArgCaptor.getValue()).expectNext(sr1).verifyComplete();
  }

  @Test
  public void registryCachesWhenProviderUpdatesServiceRegistrationsTest() {
    ServiceRegistrationProvider provider1 = mock(S3ServiceRegistrationProvider.class);

    SchemaManager schemaManager = new SchemaManager(TestHelper.testTxProvider(), mockRuntimeGraphBuilder,
        Arrays.asList(provider1), eventPublisher);

    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("test-namespace1")
        .type(Type.REST).appId("test-appId1").endpoint("endpoint1").build();
    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("test-namespace2")
        .type(Type.REST).appId("test-appId2").endpoint("endpoint2").build();

    ServiceRegistration sr1 = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();
    ServiceRegistration sr2 = ServiceRegistration.baseBuilder().serviceDefinition(sd2).build();

    ArgumentCaptor<Flux> fluxArgCaptor = ArgumentCaptor.forClass(Flux.class);
    ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);

    schemaManager.updateRegistry(provider1.getClass().getName(), Flux.just(sr1));
    verify(mockRuntimeGraphBuilder).build(fluxArgCaptor.capture(), booleanArgumentCaptor.capture());
    StepVerifier.create(fluxArgCaptor.getValue()).expectNext(sr1).verifyComplete();
  }

  @Test
  public void updateRegistryBuildsOrchestratorOnAddingServiceDefinitionTest() {
    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();

    ServiceRegistration sr = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();

    SchemaManager manager = new SchemaManager(TestHelper.testTxProvider(), mockRuntimeGraphBuilder, Collections.emptyList(),
        eventPublisher);

    manager.initializeRuntimeGraph();

    verify(mockRuntimeGraphBuilder, times(1)).build(any(Flux.class), anyBoolean());

    manager.updateRegistry("", Flux.just(sr));

    verify(mockRuntimeGraphBuilder, times(2)).build(any(Flux.class), anyBoolean());
  }

  @Test
  public void updateRegistryBuildOrchestratorOnSameServiceDefinitionSetTest() {
    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();
    ServiceDefinition sd2 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();

    ServiceRegistration sr1 = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();
    ServiceRegistration sr2 = ServiceRegistration.baseBuilder().serviceDefinition(sd2).build();

    SchemaManager manager = new SchemaManager(TestHelper.testTxProvider(), mockRuntimeGraphBuilder, Collections.emptyList(),
        eventPublisher);

    manager.initializeRuntimeGraph();

    manager.updateRegistry("test", Flux.just(sr1));
    manager.updateRegistry("test", Flux.just(sr2));

    verify(mockRuntimeGraphBuilder, times(3)).build(any(Flux.class), anyBoolean());
  }

  @Test
  public void updateRegistryBuildsOrchestratorOnRemovingServiceDefinitionTest() {
    ServiceDefinition sd1 = ServiceDefinition.newBuilder().namespace("1").type(Type.GRAPHQL).build();

    ServiceRegistration sr1 = ServiceRegistration.baseBuilder().serviceDefinition(sd1).build();

    SchemaManager manager = new SchemaManager(TestHelper.testTxProvider(), mockRuntimeGraphBuilder, Collections.emptyList(),
        eventPublisher);

    manager.initializeRuntimeGraph();

    manager.updateRegistry("test", Flux.just(sr1));
    manager.updateRegistry("test", Flux.empty());

    verify(mockRuntimeGraphBuilder, times(3)).build(any(Flux.class), anyBoolean());
  }

  @Test
  public void testRebuildsGraph() {
    when(mockRuntimeGraphBuilder.build(any())).thenReturn(Mono.just(mock(RuntimeGraph.class)));
    SchemaManager manager = new SchemaManager(TestHelper.testTxProvider(), mockRuntimeGraphBuilder, Collections.emptyList(),
        eventPublisher);
    final RuntimeGraph initialRuntimeGraph = manager.getRuntimeGraph();

    manager.rebuildGraph(TestHelper.testTx());

    assertThat(initialRuntimeGraph).isNotSameAs(manager.getRuntimeGraph());
  }

  @Test
  public void testEmitsSchemaChangeEvent() {
    when(mockRuntimeGraphBuilder.build(any())).thenReturn(Mono.just(mock(RuntimeGraph.class)));
    SchemaManager manager = new SchemaManager(TestHelper.testTxProvider(), mockRuntimeGraphBuilder, Collections.emptyList(),
        eventPublisher);

    manager.rebuildGraph(TestHelper.testTx());
    manager.updateRegistry("notUsed", Flux.empty()).block();

    verify(eventPublisher, times(2)).publishEvent(any(GraphQLSchemaChangedEvent.class));
  }
}
