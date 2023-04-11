package com.intuit.graphql.gateway.s3;

import static com.intuit.graphql.gateway.TestHelper.testTx;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.config.properties.AppLoggingProperties;
import com.intuit.graphql.gateway.config.properties.AppSecurityProperties;
import com.intuit.graphql.gateway.config.properties.WebClientProperties;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.S3ServiceRegistrationProvider;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.s3.RegistrationPoller.RegistrationResource;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.webclient.TxProvider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.S3Object;


public class RegistrationPollerTest {

  private static final Instant originalModified = Instant.now();
  private final String CONFIG_JSON;
  private final String CONFIG_JSON_WITH_DOMAINTYPES;
  private final String CONFIG_JSON_WITH_REGIONS;
  private final String CONFIG_JSON_WITH_CLIENTWHITELIST;
  private final String CONFIG_DEFAULT_TIMEOUT;
  private final String SDL_SCHEMA;
  private final String FLOW_FILE;
  private final S3Object originalRegistration;
  private final S3Object newRegistration;
  private final S3Object invalidResource;
  private final S3Object originalGraphqlResource;
  private final S3Object originalFlowResource;

  @Mock
  public SchemaRegistrationS3Client s3Client;
  @Mock
  public S3ServiceRegistrationProvider s3ServiceDefinitionProvider;

  public WebClientProperties webClientProperties;

  private RegistrationPoller registrationPoller;
  private S3Configuration s3Configuration;
  private TxProvider txProvider;
  private ServiceDefinition serviceDefinition;

  final RegistrationResource configJsonResource;
  final RegistrationResource configJsonWithRegions;

  final RegistrationResource sdlResource;

  final RegistrationResource flowResource;

  final RegistrationResource configJsonWithDomainTypes;

  final RegistrationResource configJsonWithClientWhitelist;

  final RegistrationResource serviceDefinitionResource;


  public RegistrationPollerTest() throws IOException {

    CONFIG_JSON = Resources.toString(Resources.getResource("provider-configs/config_v4os.json"), Charsets.UTF_8);
    CONFIG_JSON_WITH_DOMAINTYPES = Resources.toString(Resources.getResource(
        "provider-configs/config_v4os_with_domaintypes.json"), Charsets.UTF_8);
    CONFIG_JSON_WITH_CLIENTWHITELIST = Resources.toString(Resources.getResource(
        "provider-configs/config_v4os_with_clientwhitelist.json"), Charsets.UTF_8);
    CONFIG_JSON_WITH_REGIONS = Resources.toString(Resources.getResource(
        "provider-configs/config_v4os_with_regions.json"), Charsets.UTF_8);
    CONFIG_DEFAULT_TIMEOUT = Resources
        .toString(Resources.getResource("provider-configs/config_v4os_default_timeout.json"), Charsets.UTF_8);

    SDL_SCHEMA = Resources
        .toString(Resources.getResource("sdls/v4os_small.graphqls"), Charsets.UTF_8);

    FLOW_FILE = Resources
        .toString(Resources.getResource("test.flow"), Charsets.UTF_8);

    originalRegistration = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.test/main/config.json")
        .lastModified(originalModified.plusSeconds(1)).build();

    newRegistration = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.test/main/config.json")
        .lastModified(originalModified.plusSeconds(2)).build();

    invalidResource = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.test/main/invalid.graphqls")
        .lastModified(originalModified.plusSeconds(3)).build();

    originalGraphqlResource = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.test/main/v4os_schema.graphqls")
        .lastModified(originalModified.plusSeconds(1)).build();

    originalFlowResource = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.test/main/v4os.flow")
        .lastModified(originalModified.plusSeconds(1)).build();

    s3Configuration = new S3Configuration();
    s3Configuration.setAppName("graphql-gateway");
    s3Configuration.setEnv("dev");
    s3Configuration.setVersion("1.0.0");

    webClientProperties = new WebClientProperties();

    registrationPoller = new RegistrationPoller(s3Client,
        new TxProvider(new AppSecurityProperties(), new AppLoggingProperties()), s3Configuration);

    serviceDefinition = ServiceDefinition.newBuilder()
        .namespace("V4OS").appId("Intuit.tax.test")
        .endpoint("https://test-app-dev.api.intuit.com/graphql")
        .forwardHeaders(new HashSet<>(Arrays.asList("foo", "bar", "user_channel")))
        .timeout(4000)
        .type(Type.GRAPHQL_SDL).build();

    txProvider = new TxProvider(new AppSecurityProperties(), new AppLoggingProperties());

    configJsonResource = RegistrationResource.builder()
        .content(CONFIG_JSON.getBytes())
        .s3Object(originalRegistration)
        .build();

    sdlResource = RegistrationResource.builder()
        .content(SDL_SCHEMA.getBytes())
        .s3Object(originalGraphqlResource)
        .build();

    flowResource = RegistrationResource.builder()
        .content(FLOW_FILE.getBytes())
        .s3Object(originalFlowResource)
        .build();

    configJsonWithDomainTypes = RegistrationResource.builder()
        .content(CONFIG_JSON_WITH_DOMAINTYPES.getBytes())
        .s3Object(originalRegistration)
        .build();

    configJsonWithClientWhitelist = RegistrationResource.builder()
        .content(CONFIG_JSON_WITH_CLIENTWHITELIST.getBytes())
        .s3Object(originalRegistration)
        .build();

    configJsonWithRegions = RegistrationResource.builder()
        .content(CONFIG_JSON_WITH_REGIONS.getBytes())
        .s3Object(originalRegistration)
        .build();

    serviceDefinitionResource = RegistrationResource.builder()
        .content(serviceDefinition.toString().getBytes())
        .s3Object(null)
        .build();
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(s3ServiceDefinitionProvider.update()).thenReturn(Mono.empty());
    registrationPoller = new RegistrationPoller(s3Client, txProvider, s3Configuration);
    s3Configuration.getPolling().setSyncDelay(Duration.ofSeconds(-1));
  }

  @Test
  public void testFetch() {
    when(s3Client.listRegistrations()).thenReturn(Flux.empty());
    registrationPoller.fetch(null, testTx()).block();
    verify(s3Client).listRegistrations();
  }

  @Test
  public void testSync() {

    when(s3Client.listRegistrations())
        .thenReturn(Flux.just(originalRegistration, originalGraphqlResource, originalFlowResource));

    when(s3Client.downloadRegistrationResource(eq(originalRegistration)))
        .thenReturn(Mono.just(configJsonResource));

    when(s3Client.downloadRegistrationResource(eq(originalGraphqlResource)))
        .thenReturn(Mono.just(sdlResource));

    when(s3Client.downloadRegistrationResource(eq(originalFlowResource)))
        .thenReturn(Mono.just(flowResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    doReturn(configJsonResource, sdlResource, flowResource)
        .when(s3ServiceDefinitionProvider).cache(any(), any());

    verify(s3ServiceDefinitionProvider, times(3))
        .cache(any(TransactionContext.class), any(RegistrationResource.class));

    assertThat(
        registrationPoller.registrationCache.get(originalRegistration.key()))
        .isSameAs(originalRegistration);
    assertThat(
        registrationPoller.registrationCache.get(originalGraphqlResource.key()))
        .isSameAs(originalGraphqlResource);
    assertThat(
        registrationPoller.registrationCache.get(originalFlowResource.key()))
        .isSameAs(originalFlowResource);
  }

  @Test
  public void testDefaultTimeout() {

    final RegistrationResource configResource = RegistrationResource.builder()
        .content(CONFIG_DEFAULT_TIMEOUT.getBytes())
        .s3Object(null)
        .build();

    ServiceDefinition tServiceDefinition = ServiceDefinition.newBuilder()
        .namespace("V4OS").appId("Intuit.tax.test")
        .endpoint("https://test-app-dev.api.intuit.com/graphql")
        .timeout(10000)
        .type(Type.GRAPHQL_SDL).build();

    when(s3Client.listRegistrations())
        .thenReturn(Flux.just(originalRegistration));

    when(s3Client.downloadRegistrationResource(eq(originalRegistration)))
        .thenReturn(Mono.just(configResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider) //todo
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
  }

  @Test
  public void testHeaderWhiteList() {

    when(s3Client.listRegistrations())
        .thenReturn(Flux.just(originalRegistration));

    when(s3Client.downloadRegistrationResource(eq(originalRegistration)))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider)
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
  }


  @Test
  public void testExistsInCacheFilter() {
    when(s3Client.listRegistrations()).thenReturn(Flux.just(originalRegistration));

    registrationPoller.registrationCache.put(originalRegistration.key(), originalRegistration);

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3Client, never()).downloadRegistrationResource(any());
  }


  @Test
  public void testNewRegistration() {
    when(s3Client.listRegistrations()).thenReturn(Flux.just(newRegistration));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.registrationCache.put(originalRegistration.key(), originalRegistration);

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider, atLeastOnce())
        .cache(any(TransactionContext.class), any(RegistrationResource.class));

    assertThat(registrationPoller.registrationCache.get(newRegistration.key())).isSameAs(newRegistration);
  }

  @Test
  public void testDeleteInvalidRegistration() {
    when(s3Client.listRegistrations()).thenReturn(Flux.just(newRegistration));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.registrationCache.put(invalidResource.key(), invalidResource);
    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    ArgumentCaptor<RegistrationResource> resourceArgumentCaptor = ArgumentCaptor.forClass(RegistrationResource.class);

    verify(s3ServiceDefinitionProvider, times(1))
        .cache(any(TransactionContext.class), resourceArgumentCaptor.capture());
    verify(s3ServiceDefinitionProvider, atLeastOnce())
        .delete(any(TransactionContext.class),
            eq("graphql-gateway/test/registrations/1.0.0/Intuit.tax.test/main/invalid.graphqls"));
    assertThat(resourceArgumentCaptor.getValue().isMainConfigFile()).isTrue();
    assertThat(registrationPoller.registrationCache.get(invalidResource.key())).isSameAs(null);
    assertThat(registrationPoller.registrationCache.get(newRegistration.key())).isSameAs(newRegistration);
  }


  @Test
  public void testConfigJsonWithDomainTypes() {
    S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(registrationPoller,
        this.s3Configuration, txProvider, webClientProperties);
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonWithDomainTypes));

    S3Object s3Object = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.testdomaintypes/main/config.json")
        .lastModified(Instant.now()).build();

    when(s3Client.listRegistrations()).thenReturn(Flux.just(s3Object));
    registrationPoller.fetch(s3ServiceRegistrationProvider, testTx()).block();
    ServiceRegistration serviceRegistration = s3ServiceRegistrationProvider.getInitialServiceRegistrations()
        .blockFirst();
    assertThat(serviceRegistration.getServiceDefinition().getDomainTypes())
        .contains("TaxType", "ReturnDataType");
  }

  @Test
  public void testConfigJsonWithMinTimeout() {
    s3Configuration.setRegion(Region.US_WEST_2);
    WebClientProperties webClientProperties1 = new WebClientProperties();
    webClientProperties1.setTimeout(2000);
    S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(registrationPoller,
        this.s3Configuration, txProvider, webClientProperties1);
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonWithRegions));

    S3Object s3Object = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.testdomaintypes/main/config.json")
        .lastModified(Instant.now()).build();

    when(s3Client.listRegistrations()).thenReturn(Flux.just(s3Object));
    registrationPoller.fetch(s3ServiceRegistrationProvider, testTx()).block();
    ServiceRegistration serviceRegistration = s3ServiceRegistrationProvider.getInitialServiceRegistrations()
        .blockFirst();
    assertThat(serviceRegistration.getServiceDefinition().getEndpoint())
        .isEqualTo("https://test-app-dev-west.api.intuit.com/graphql");
    assertThat(serviceRegistration.getServiceDefinition().getTimeout()).isEqualTo(2000);
  }

  @Test
  public void testConfigJsonWithRegions() {
    s3Configuration.setRegion(Region.US_WEST_2);
    S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(registrationPoller,
        this.s3Configuration, txProvider, webClientProperties);
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonWithRegions));

    S3Object s3Object = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.testdomaintypes/main/config.json")
        .lastModified(Instant.now()).build();

    when(s3Client.listRegistrations()).thenReturn(Flux.just(s3Object));
    registrationPoller.fetch(s3ServiceRegistrationProvider, testTx()).block();
    ServiceRegistration serviceRegistration = s3ServiceRegistrationProvider.getInitialServiceRegistrations()
        .blockFirst();
    assertThat(serviceRegistration.getServiceDefinition().getEndpoint())
        .isEqualTo("https://test-app-dev-west.api.intuit.com/graphql");
    assertThat(serviceRegistration.getServiceDefinition().getTimeout()).isEqualTo(5001);
  }

  @Test
  public void testConfigJsonWithClientWhitelist() {
    S3ServiceRegistrationProvider s3ServiceRegistrationProvider = new S3ServiceRegistrationProvider(registrationPoller,
        this.s3Configuration, txProvider, webClientProperties);
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonWithClientWhitelist));

    S3Object s3Object = S3Object.builder()
        .key("graphql-gateway/test/registrations/1.0.0/Intuit.tax.testdomaintypes/main/config.json")
        .lastModified(Instant.now()).build();

    when(s3Client.listRegistrations()).thenReturn(Flux.just(s3Object));
    registrationPoller.fetch(s3ServiceRegistrationProvider, testTx()).block();
    ServiceRegistration serviceRegistration = s3ServiceRegistrationProvider.getInitialServiceRegistrations()
        .blockFirst();
    assertThat(serviceRegistration.getServiceDefinition().getClientWhitelist())
        .contains("intuit.services.test-service", "intuit.ios.test-client");
  }

  @Test
  public void testSyncRegistrationErrorContinues() {
    when(s3Client.listRegistrations()).thenReturn(Flux.just(originalRegistration, newRegistration,
        newRegistration.copy(builder -> builder.lastModified(originalModified.plusSeconds(3)))));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource))
        .thenReturn(Mono.error(new RuntimeException("Something bad happened")))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider, times(2))
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
    verify(s3Client, times(3)).downloadRegistrationResource(any());
  }

  @Test
  public void testFetchRegistrationErrorContinues() {
    when(s3Client.listRegistrations()).thenReturn(Flux.just(originalRegistration, newRegistration,
        newRegistration.copy(builder -> builder.lastModified(originalModified.plusSeconds(3)))));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource))
        .thenReturn(Mono.error(new RuntimeException("Something bad happened")))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.fetch(s3ServiceDefinitionProvider, testTx()).block();

    verify(s3ServiceDefinitionProvider, times(2))
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
    verify(s3Client, times(3)).downloadRegistrationResource(any());
  }


  @Test
  public void testSequenceContinuesWithSubsequentItems() {
    when(s3Client.listRegistrations())
        .thenReturn(Flux.just(originalRegistration, S3Object.builder().key(null).build(), newRegistration));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(serviceDefinitionResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3Client, times(2)).downloadRegistrationResource(any());
  }

  @Test
  public void testUpdateProvidersCallWhenNoFileFetched() {
    when(s3Client.listRegistrations()).thenReturn(Flux.just(originalRegistration, newRegistration,
        newRegistration.copy(builder -> builder.lastModified(originalModified.plusSeconds(3)))));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.error(new RuntimeException("Something bad happened")))
        .thenReturn(Mono.error(new RuntimeException("Something bad happened")))
        .thenReturn(Mono.error(new RuntimeException("Something bad happened")));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider, never()).update();
    verify(s3Client, times(3)).downloadRegistrationResource(any());
  }

  static {
    Hooks.onOperatorDebug();
  }

  @Test
  public void testUpdateProvidersCallWhenFileFetched() {
    when(s3ServiceDefinitionProvider.cache(any(), any())).thenReturn(configJsonResource);
    when(s3Client.listRegistrations()).thenReturn(Flux.just(originalRegistration, newRegistration,
        newRegistration.copy(builder -> builder.lastModified(originalModified.plusSeconds(3)))));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider, times(1)).update();
    verify(s3Client, times(3)).downloadRegistrationResource(any());
    verify(s3ServiceDefinitionProvider, times(3))
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
  }

  @Test
  public void testUpdateProvidersCallOnErrorContinue() {
    when(s3ServiceDefinitionProvider.cache(any(), any())).thenReturn(configJsonResource);
    when(s3Client.listRegistrations()).thenReturn(Flux.just(originalRegistration, newRegistration,
        newRegistration.copy(builder -> builder.lastModified(originalModified.plusSeconds(3)))));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource))
        .thenReturn(Mono.error(new RuntimeException("Something bad happened")))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();

    verify(s3ServiceDefinitionProvider, times(1)).update();
    verify(s3Client, times(3)).downloadRegistrationResource(any());
    verify(s3ServiceDefinitionProvider, times(2))
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
  }

  //todo this is a fragile test that relies on real clock time.
//  @Test
//  public void testSyncDelaySkipsDownloads() {
//    when(s3ServiceDefinitionProvider.cache(any(), any())).thenReturn(configJsonResource);
//    //this test might fail while debugging
//    s3Configuration.setSyncDelay(Duration.ofSeconds(2));
//
//    when(s3Client.listRegistrations())
//        .thenReturn(Flux.just(
//            newRegistration.copy(builder -> builder.lastModified(originalModified.minusSeconds(3))),
//            newRegistration.copy(builder -> builder.lastModified(originalModified.minusSeconds(2))),
//            newRegistration.copy(builder -> builder.lastModified(originalModified.minusSeconds(0))),
//            newRegistration.copy(builder -> builder.lastModified(originalModified.plusSeconds(1)))
//        ));
//    when(s3Client.downloadRegistrationResource(any()))
//        .thenReturn(Mono.just(configJsonResource));
//
//    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();
//    verify(s3ServiceDefinitionProvider, times(2))
//        .cache(any(TransactionContext.class), any(RegistrationResource.class));
//    verify(s3Client, times(2)).downloadRegistrationResource(any());
//  }

  @Test
  public void testSyncDelayDownloadsAfterDelay() {
    //this test might fail while debugging
    s3Configuration.getPolling().setSyncDelay(Duration.ofSeconds(1));

    when(s3Client.listRegistrations())
        .thenReturn(Flux.just(
            newRegistration.copy(builder -> builder.lastModified(originalModified.minusSeconds(3))),
            newRegistration.copy(builder -> builder.lastModified(originalModified.minusSeconds(2))),
            newRegistration.copy(builder -> builder.lastModified(originalModified.minusSeconds(1)))
        ));
    when(s3Client.downloadRegistrationResource(any()))
        .thenReturn(Mono.just(configJsonResource));

    registrationPoller.buildPollingSequence(s3ServiceDefinitionProvider).block();
    verify(s3ServiceDefinitionProvider, times(3))
        .cache(any(TransactionContext.class), any(RegistrationResource.class));
    verify(s3Client, times(3)).downloadRegistrationResource(any());
  }
}
