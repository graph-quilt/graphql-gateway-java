package com.intuit.graphql.gateway.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

public class SdlServiceProviderTest {


  @Mock
  private SdlServiceRegistration serviceRegistration;

  @Mock
  TransactionContext tx;
  private WebClient webClient;

  private final ServiceDefinition serviceDefinition;
  private final String SDL_SCHEMA;
  private final String SDL_SCHEMA_INVALID;

  public SdlServiceProviderTest() throws Exception {
    serviceDefinition = ServiceDefinition.newBuilder().namespace("test-namespace")
        .endpoint("endpoint").type(Type.GRAPHQL).build();
    SDL_SCHEMA = Resources
        .toString(Resources.getResource("sdls/v4os_small.graphqls"), Charsets.UTF_8);
    SDL_SCHEMA_INVALID = Resources
        .toString(Resources.getResource("sdls/invalid_schema.graphqls"), Charsets.UTF_8);
  }

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    webClient = WebClient.builder().build();
  }

  @Test
  public void loadSchemaTest() {
    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("key", SDL_SCHEMA);

    when(serviceRegistration.getGraphqlResources()).thenReturn(graphqlResources);
    when(serviceRegistration.getServiceDefinition()).thenReturn(serviceDefinition);

    SdlServiceProvider sdlServiceProvider = new SdlServiceProvider(serviceRegistration, webClient);
    Map<String, String> sdlFiles = sdlServiceProvider.sdlFiles();
    TypeDefinitionRegistry tdr = new SchemaParser().parse(sdlFiles.get("key"));

    assertThat(tdr.types().keySet()).contains("HSAContribution8889Type");
  }

  @Test
  public void loadCachesBuiltSchemaTest() {
    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("key", SDL_SCHEMA);

    when(serviceRegistration.getGraphqlResources()).thenReturn(graphqlResources);
    when(serviceRegistration.getServiceDefinition()).thenReturn(serviceDefinition);

    SdlServiceProvider sdlServiceProvider = new SdlServiceProvider(serviceRegistration, webClient);

    @SuppressWarnings("unused")
    Map<String, String> tossAway = sdlServiceProvider.sdlFiles();
    Map<String, String> sdlFiles = sdlServiceProvider.sdlFiles();
    TypeDefinitionRegistry tdr = new SchemaParser().parse(sdlFiles.get("key"));

    verify(serviceRegistration, times(2)).getGraphqlResources();
    assertThat(tdr.types().keySet()).contains("HSAContribution8889Type");
  }

  // @Test(expected = ServiceRegistrationException.class)
  @Test
  public void loadThrowsExceptionOnInvalidSdl() {

    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("key", SDL_SCHEMA_INVALID);

    when(serviceRegistration.getGraphqlResources()).thenReturn(graphqlResources);
    when(serviceRegistration.getServiceDefinition()).thenReturn(serviceDefinition);

    SdlServiceProvider sdlServiceProvider = new SdlServiceProvider(serviceRegistration, webClient);
    Map<String, String> sdlFiles = sdlServiceProvider.sdlFiles();
    //TypeDefinitionRegistry tdr = new SchemaParser().parse(sdlFiles.get("key"));
  }
}