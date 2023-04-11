package com.intuit.graphql.gateway.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.config.properties.WebClientProperties;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.s3.ImmutableS3ServiceDefinition;
import com.intuit.graphql.gateway.s3.RegistrationPoller;
import com.intuit.graphql.gateway.s3.RegistrationPoller.RegistrationResource;
import com.intuit.graphql.gateway.s3.S3Configuration;
import com.intuit.graphql.gateway.s3.S3Configuration.S3Polling;
import com.intuit.graphql.gateway.s3.S3Poller;
import com.intuit.graphql.gateway.s3.S3RegistrationCache;
import com.intuit.graphql.gateway.s3.S3Registry;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition;
import com.intuit.graphql.gateway.webclient.TxProvider;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.TestHelper;
import java.time.Duration;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3ServiceRegistrationProviderTests {

  TxProvider txProvider = TestHelper.testTxProvider();

  @Mock
  private RegistrationPoller s3Poller;
  @Mock
  private WebClientProperties webClientProperties;

  private S3Configuration s3Configuration;

  private S3ServiceRegistrationProvider s3ServiceRegistrationProvider;

  private VirtualTimeScheduler globalScheduler;
  private TestPoller testPoller;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    s3Configuration = new S3Configuration();
    s3Configuration.setEnabled(true);
    s3Configuration.setEnv("e2e");
    s3Configuration.setPolling(Mockito.spy(new S3Polling()));
    this.globalScheduler = VirtualTimeScheduler.create();
    this.testPoller = new TestPoller(globalScheduler);
    this.s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(s3Poller, s3Configuration, txProvider, webClientProperties);
  }

  @Test
  @SneakyThrows
  public void syncWithSameIdOverridesExistingProvidersTest() {
    final String s3Key = "graphql-gateway/e2e/registrations/1.0.0/appid/main/config.json";
    final String expectedRegistrationId = "appid";
    S3ServiceDefinition sd1 = Mapper.mapper()
        .readValue(TestHelper.read("provider-configs/config_v4os.json"), S3ServiceDefinition.class);
    S3ServiceDefinition sd2 = ImmutableS3ServiceDefinition.copyOf(sd1).withNamespace("updated_namespace");

    RegistrationResource resource = RegistrationResource.builder()
        .s3Object(S3Object.builder().key(s3Key).build())
        .content(Mapper.mapper().writeValueAsBytes(sd1))
        .build();

    RegistrationResource updatedResource = RegistrationResource.builder()
        .s3Object(resource.getS3Object()).content(Mapper.mapper().writeValueAsBytes(sd2)).build();

    s3ServiceRegistrationProvider.cache(TestHelper.testTx(), resource);
    s3ServiceRegistrationProvider.cache(TestHelper.testTx(), updatedResource);

    assertThat(s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.size()).isEqualTo(1);
    assertThat(
        s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.get(expectedRegistrationId).getServiceDefinition()
            .getNamespace()).isEqualTo("updated_namespace");
  }

  @Test
  @SneakyThrows
  public void deletingConfigJsonRemovesProviderFromRegistryCacheTest() {
    final String registrationId = "appid";
    final S3RegistrationCache cache1 = new S3RegistrationCache();
    cache1.setServiceDefinition(ServiceDefinition.newBuilder().build());
    cache1.addFlowResource("graphql-gateway/e2e/registrations/1.0.0/appid/main/flow1.flow", "dummyFlow");
    cache1.addGraphqlResource("graphql-gateway/e2e/registrations/1.0.0/appid/main/schema.graphqls", "dummySchema");

    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.put(registrationId, cache1);
    assertThat(s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.size()).isEqualTo(1);

    s3ServiceRegistrationProvider.delete(TestHelper.testTx(), "graphql-gateway/e2e/registrations/1.0.0/appid/main/config.json");
    s3ServiceRegistrationProvider.delete(TestHelper.testTx(), "graphql-gateway/e2e/registrations/1.0.0/appid2/main/flow2.flow");

    assertThat(s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.get(registrationId)).isNull();
    assertThat(s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.size()).isEqualTo(0);
  }

  @Test
  @SneakyThrows
  public void deletingResourceOnlyDoesNotRemoveRegistrationFromCacheTest() {
    final String registrationId2 = "appid2";
    final S3RegistrationCache cache2 = new S3RegistrationCache();
    cache2.setServiceDefinition(ServiceDefinition.newBuilder().build());
    cache2.addFlowResource("graphql-gateway/e2e/registrations/1.0.0/appid2/main/flow2.flow", "dummyFlow");
    cache2.addGraphqlResource("graphql-gateway/e2e/registrations/1.0.0/appid2/main/schema2.graphqls", "dummySchema");

    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.put(registrationId2, cache2);

    assertThat(s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.size()).isEqualTo(1);
    assertThat(
        s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.get(registrationId2).getObjectKeyFlowResourceMap()
            .size()).isEqualTo(1);
    s3ServiceRegistrationProvider.delete(TestHelper.testTx(), "graphql-gateway/e2e/registrations/1.0.0/appid2/main/flow2.flow");
    assertThat(
        s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.get(registrationId2).getObjectKeyFlowResourceMap()
            .size()).isEqualTo(0);
    assertThat(s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.size()).isEqualTo(1);
  }

  @Test
  public void getInitialProvidersGivesAllUniqueServiceDefinitionsTest() {
    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap
        .put("test1", new S3RegistrationCache()
            .setServiceDefinition(ServiceDefinition.newBuilder().namespace("name1").type(Type.GRAPHQL).build()));
    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap
        .put("test2", new S3RegistrationCache()
            .setServiceDefinition(ServiceDefinition.newBuilder().namespace("name2").type(Type.GRAPHQL).build()));
    when(s3Poller.fetch(any(), any())).thenReturn(Mono.empty());
    StepVerifier.create(s3ServiceRegistrationProvider.getInitialServiceRegistrations().count()).expectNext(2L)
        .verifyComplete();
  }

  @Test
  public void getInitialServiceRegistrationsAllowsDuplicatesTest() {
    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.put("test1", new S3RegistrationCache()
        .setServiceDefinition(ServiceDefinition.newBuilder().namespace("name").type(Type.REST).build()));
    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.put("test2", new S3RegistrationCache()
        .setServiceDefinition(ServiceDefinition.newBuilder().namespace("name").type(Type.REST).build()));
    when(s3Poller.fetch(any(), any())).thenReturn(Mono.empty());
    StepVerifier.create(s3ServiceRegistrationProvider.getInitialServiceRegistrations().count()).expectNext(2L)
        .verifyComplete();
  }

  @Test
  public void updateProvidersUpdatesRegistryTest() {
    ServiceRegistry serviceRegistry = Mockito.mock(ServiceRegistry.class);

    s3ServiceRegistrationProvider.registerServiceRegistry(serviceRegistry);
    s3ServiceRegistrationProvider.update();

    verify(serviceRegistry).updateRegistry(any(), any());
  }

  @Test
  public void getInitialServiceRegistrationsCallsS3PollerFetchTest() {
    when(s3Poller.fetch(any(), any())).thenReturn(Mono.empty());

    s3ServiceRegistrationProvider.getInitialServiceRegistrations().blockFirst();

    verify(s3Poller).fetch(eq(s3ServiceRegistrationProvider), any());
  }

  @Test
  public void beginPollingChecksIfPollingIsEnabledTest() {
    when(s3Poller.fetch(any(), any())).thenReturn(Mono.empty());
    s3ServiceRegistrationProvider.beginPolling();
    verify(s3Configuration.getPolling()).isEnabled();
  }

  @Test
  public void beginPollingStartsPollingIfEnabledTest() {

    final S3Configuration configuration = new S3Configuration();
    configuration.setEnabled(true);
    configuration.getPolling().setPeriod(Duration.ofSeconds(2));
    configuration.getPolling().setEnabled(true);

    final S3ServiceRegistrationProvider registrationProvider = new S3ServiceRegistrationProvider(testPoller,
        configuration, txProvider, webClientProperties);
    registrationProvider.beginPolling();

    globalScheduler.advanceTimeBy(Duration.ofSeconds(2));
    assertThat(testPoller.subscriptions).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void beginPollingDoesntStartPollingIfDisabledTest() {
    when(s3Poller.fetch(any(), any())).thenReturn(Mono.empty());
    s3ServiceRegistrationProvider.beginPolling();
    verify(s3Poller, never()).buildPollingSequence(any());
  }


  @Test
  public void testSchedulerRepeats() {
    final S3Configuration s3Configuration = new S3Configuration();
    s3Configuration.setEnabled(true);
    s3Configuration.getPolling().setPeriod(Duration.ofSeconds(1));
    s3Configuration.getPolling().setEnabled(true);

    final S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(testPoller,
        s3Configuration, txProvider, webClientProperties);
    s3ServiceRegistrationProvider.beginPolling();
    globalScheduler.advanceTimeBy(Duration.ofSeconds(4));

    assertThat(testPoller.subscriptions).isEqualTo(4);

    //stops subsequent subscriptions
    s3Configuration.getPolling().setEnabled(false);

    globalScheduler.advanceTimeBy(Duration.ofSeconds(4));

    assertThat(testPoller.subscriptions).isEqualTo(5);
  }

  @Test
  public void testSchedulerRepeatsOnError() {
    final S3Configuration s3Configuration = new S3Configuration();
    s3Configuration.setEnabled(true);
    s3Configuration.getPolling().setPeriod(Duration.ofSeconds(1));
    s3Configuration.getPolling().setEnabled(true);

    ErrorPoller errorPoller = new ErrorPoller(
        globalScheduler);

    final S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(errorPoller,
        s3Configuration, txProvider, webClientProperties);
    s3ServiceRegistrationProvider.beginPolling();

    globalScheduler.advanceTimeBy(Duration.ofSeconds(2));

    assertThat(errorPoller.subscriptions).isEqualTo(2);
  }

  @Test
  public void testSchedulerUpdatesOnChangeInPollingPeriod() {
    final S3Configuration s3Configuration = new S3Configuration();
    s3Configuration.setEnabled(true);
    s3Configuration.getPolling().setPeriod(Duration.ofSeconds(2));
    s3Configuration.getPolling().setEnabled(true);

    final S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(testPoller,
        s3Configuration, txProvider, webClientProperties);
    s3ServiceRegistrationProvider.beginPolling();
    globalScheduler.advanceTimeBy(Duration.ofSeconds(8));

    assertThat(testPoller.subscriptions).isEqualTo(4);

    s3Configuration.getPolling().setPeriod(Duration.ofSeconds(1));

    globalScheduler.advanceTimeBy(Duration.ofSeconds(8));

    assertThat(testPoller.subscriptions).isEqualTo(11);
  }


  @Test
  public void testCacheServiceDefinition() {

    final String s3Key = "graphql-gateway/e2e/registrations/1.0.0/appid/main/config.json";
    final String expectedRegistrationId = "appid";
    final String serviceRegistrationResource = new String(TestHelper.read("provider-configs/config_v4os.json"));

    final RegistrationResource registrationResource = RegistrationResource.builder()
        .s3Object(S3Object.builder().key(s3Key).build())
        .content(serviceRegistrationResource.getBytes()).build();

    s3ServiceRegistrationProvider.cache(TestHelper.testTx(), registrationResource);

    S3RegistrationCache cachedObject = s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap
        .get(expectedRegistrationId);

    assertThat(cachedObject.getServiceDefinition()).isNotNull();
  }


  @Test
  public void testCacheGraphqlResource() {
    final String s3Key = "graphql-gateway/e2e/registrations/1.0.0/appid/graphqlfile.graphqls";
    final String expectedRegistrationId = "appid";
    final String graphqlSchemaResource = "test";

    final RegistrationResource registrationResource = RegistrationResource.builder()
        .s3Object(S3Object.builder().key(s3Key).build())
        .content(graphqlSchemaResource.getBytes()).build();

    s3ServiceRegistrationProvider.cache(TestHelper.testTx(), registrationResource);

    S3RegistrationCache cachedObject = s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap
        .get(expectedRegistrationId);
    assertThat(cachedObject.getObjectKeyGraphqlResourceMap().get(s3Key)).isEqualTo(graphqlSchemaResource);
  }

  @Test
  public void testCacheFlowResource() {
    final String s3Key = "graphql-gateway/e2e/registrations/1.0.0/appid/flow/service.flow";
    final String expectedRegistrationKey = "appid";
    final String flowResourceContent = "test";

    S3RegistrationCache original = new S3RegistrationCache();
    s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap.put(expectedRegistrationKey, original);

    RegistrationResource resource = RegistrationResource.builder()
        .s3Object(S3Object.builder().key(s3Key).build())
        .content(flowResourceContent.getBytes())
        .build();

    s3ServiceRegistrationProvider.cache(TestHelper.testTx(), resource);

    S3RegistrationCache cachedObject = s3ServiceRegistrationProvider.regIdS3RegistrationCacheMap
        .get(expectedRegistrationKey);
    assertThat(cachedObject).isSameAs(original);
    assertThat(cachedObject.getObjectKeyFlowResourceMap().get(s3Key)).isEqualTo(flowResourceContent);
  }

  private static class ErrorPoller implements S3Poller<RegistrationResource> {

    int subscriptions = 0;

    private VirtualTimeScheduler virtualTimeScheduler;

    public ErrorPoller(VirtualTimeScheduler scheduler) {
      this.virtualTimeScheduler = scheduler;
    }

    @Override
    public Mono<Void> fetch(S3Registry<RegistrationResource> registry, TransactionContext tx) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> buildPollingSequence(final S3Registry<RegistrationResource> registry) {
      return Flux.just(1)
          .doOnSubscribe(subscription -> subscriptions++)
          .then(Mono.error(new RuntimeException("Something bad happened!")));
    }

    @Override
    public Scheduler getDefaultScheduler() {
      return virtualTimeScheduler;
    }

  }

  private static class TestPoller implements S3Poller<RegistrationResource> {

    int subscriptions = 0;

    private VirtualTimeScheduler virtualTimeScheduler;

    TestPoller(VirtualTimeScheduler scheduler) {
      this.virtualTimeScheduler = scheduler;
    }

    @Override
    public Mono<Void> fetch(S3Registry<RegistrationResource> registry, TransactionContext tx) {
      return Mono.empty();
    }

    @Override
    public Mono<Void> buildPollingSequence(final S3Registry<RegistrationResource> registry) {
      return Mono.empty()
          .doOnSubscribe(subscription -> subscriptions++)
          .then();
    }

    @Override
    public Scheduler getDefaultScheduler() {
      return virtualTimeScheduler;
    }
  }
}
