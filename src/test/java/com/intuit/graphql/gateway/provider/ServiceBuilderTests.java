package com.intuit.graphql.gateway.provider;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import com.intuit.graphql.orchestrator.ServiceProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

public class ServiceBuilderTests {

  ServiceBuilder serviceBuilder;

  WebClient webClient;

  MockWebServer mockWebServer;
  ServiceDefinition serviceDefinition;

  private final String INTROSPECTED_SCHEMA;
  private final String SDL_SCHEMA;
  private final String SDL_SCHEMA_INVALID;
  private final String INTROSPECTED_SCHEMA_INVALID;

  public ServiceBuilderTests() throws Exception {
    this.SDL_SCHEMA = Resources
        .toString(Resources.getResource("sdls/v4os_small.graphqls"), Charsets.UTF_8);
    this.INTROSPECTED_SCHEMA = Resources
        .toString(Resources.getResource("introspections/v4os_small.json"), Charsets.UTF_8);
    this.INTROSPECTED_SCHEMA_INVALID = Resources
        .toString(Resources.getResource("introspections/invalid_introspection.json"), Charsets.UTF_8);
    this.SDL_SCHEMA_INVALID = Resources
        .toString(Resources.getResource("sdls/invalid_schema.graphqls"), Charsets.UTF_8);
  }

  @Before
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.webClient = WebClient.builder().build();
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    serviceBuilder = new ServiceBuilder(webClient);
    serviceDefinition = ServiceDefinition.newBuilder().namespace("1").endpoint(mockWebServer.url("/test").toString())
        .type(Type.GRAPHQL).build();
  }

  @After
  public void teardown() throws Exception {
    mockWebServer.shutdown();
  }

  public static byte[] getSampleRegistryFile(String filename) throws IOException {
    File file = new File(
        ServiceBuilderTests.class.getClassLoader().getResource(filename).getFile()
    );
    return Files.readAllBytes(file.toPath());
  }

//  @Test(expected = ServiceRegistrationException.class)
//  public void buildServiceRequiresNonNullNamespaceTest() {
//    ServiceRegistration serviceRegistration = ServiceRegistration.baseBuilder()
//        .serviceDefinition(serviceDefinition)
//        .build();
//    serviceBuilder.buildService(TestHelper.testTx(), serviceRegistration);
//  }

  @Test(expected = ServiceRegistrationException.class)
  public void buildServiceRequiresNonEmptyNamespaceTest() {
    ServiceDefinition tServiceDefinition = ServiceDefinition.newBuilder()
        .namespace("").type(Type.GRAPHQL).build();
    ServiceRegistration registrationPackage = ServiceRegistration.baseBuilder()
        .serviceDefinition(tServiceDefinition)
        .build();
    serviceBuilder.buildService(TestHelper.testTx(), registrationPackage);
  }


  @Test
  public void buildServiceReturnsServiceProviderOnValidIntrospectionTest() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTED_SCHEMA));

    ServiceRegistration serviceRegistration = ServiceRegistration.baseBuilder()
        .serviceDefinition(serviceDefinition)
        .build();

    ServiceProvider source = serviceBuilder.buildService(TestHelper.testTx(), serviceRegistration);
    assertThat(source.sdlFiles().size()).isEqualTo(1);
  }

  @Test
  public void buildServiceReturnsServiceProviderOnValidSDLSchemaTest() {

    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("key", SDL_SCHEMA);

    ServiceRegistration serviceRegistration = SdlServiceRegistration.builder()
        .serviceDefinition(serviceDefinition).graphqlResources(graphqlResources)
        .build();

    ServiceProvider source = serviceBuilder.buildService(TestHelper.testTx(), serviceRegistration);
    assertThat(source.sdlFiles().size()).isEqualTo(1);
  }

/*  This test case is failing as we no longer parse the SDL files to convert them into TDR.
  @Test(expected = ServiceRegistrationException.class)
  public void buildSchemaSourceThrowsExceptionOnInvalidSdlTest() {
    ServiceDefinition serviceDefinition = new ServiceDefinition();
    serviceDefinition.setNamespace("test");
    serviceDefinition.setEndpoint("endpoint");

    Map<String, String> graphqlResources = new HashMap<>();
    graphqlResources.put("key", SDL_SCHEMA_INVALID);

    ServiceRegistration serviceRegistration = SdlServiceRegistration.builder()
        .serviceDefinition(serviceDefinition).graphqlResources(graphqlResources)
        .build();

    serviceBuilder.buildService(testTx(), serviceRegistration).sdlFiles();
  }*/

  @Test(expected = ServiceRegistrationException.class)
  public void buildSchemaSourceThrowsExceptionOnInvalidIntrospectionTest() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTED_SCHEMA_INVALID));

    ServiceRegistration serviceRegistration = ServiceRegistration.baseBuilder()
        .serviceDefinition(serviceDefinition)
        .build();

    serviceBuilder.buildService(TestHelper.testTx(), serviceRegistration).sdlFiles();
  }

  @Test(expected = ServiceRegistrationException.class)
  public void buildSchemaSourceThrowsExceptionOnIntrospectionFailureTest() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));
    ServiceRegistration serviceRegistration = ServiceRegistration.baseBuilder()
        .serviceDefinition(serviceDefinition)
        .build();

    serviceBuilder.buildService(TestHelper.testTx(), serviceRegistration).sdlFiles();
  }

}
