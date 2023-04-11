package com.intuit.graphql.gateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

public class IntrospectionServiceProviderTest {

  private MockWebServer mockWebServer;
  private WebClient webClient;
  private ServiceDefinition serviceDefinition;
  private ServiceRegistration serviceRegistration;
  private final String INTROSPECTED_SCHEMA;
  private final String INTROSPECTION_WITH_DESCRIPTION;
  private final String INTROSPECTED_SCHEMA_INVALID;

  public IntrospectionServiceProviderTest() throws Exception {
    INTROSPECTED_SCHEMA = Resources
        .toString(Resources.getResource("introspections/v4os_small.json"), Charsets.UTF_8);
    INTROSPECTED_SCHEMA_INVALID = Resources
        .toString(Resources.getResource("introspections/invalid_introspection.json"), Charsets.UTF_8);
    INTROSPECTION_WITH_DESCRIPTION = Resources
        .toString(Resources.getResource("introspections/introspection_with_description.json"), Charsets.UTF_8);
  }

  @Before
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.mockWebServer = new MockWebServer();
    this.webClient = WebClient.builder().build();
    this.mockWebServer.start();
    this.serviceDefinition = ServiceDefinition.newBuilder().namespace("test")
        .endpoint(mockWebServer.url("/test").toString()).type(Type.GRAPHQL).build();
    this.serviceRegistration = ServiceRegistration.baseBuilder()
        .serviceDefinition(serviceDefinition)
        .build();
  }

  @After
  public void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  public void loadGetsRemoteSchemaTest() {

    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTED_SCHEMA));

    IntrospectionServiceProvider introspectionServiceProvider = new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    );
    assertThat(introspectionServiceProvider.getNameSpace()).isEqualTo(serviceDefinition.getNamespace());
    assertThat(introspectionServiceProvider.domainTypes()).isEqualTo(serviceDefinition.getDomainTypes());

    Map<String, String> sdlFiles = introspectionServiceProvider.sdlFiles();
    assertThat(sdlFiles.containsKey(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME)).isTrue();
    TypeDefinitionRegistry tdr = new SchemaParser()
        .parse(sdlFiles.get(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME));

    assertThat(tdr.types().keySet()).contains("AccountType");
  }

  @Test
  public void loadCachesRemoteSchemaTest() {

    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTED_SCHEMA));

    IntrospectionServiceProvider introspectionServiceProvider = new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    );

    @SuppressWarnings("unused")

    Map<String, String> tossAway = introspectionServiceProvider.sdlFiles();
    Map<String, String> sdlFiles = introspectionServiceProvider.sdlFiles();
    assertThat(sdlFiles.containsKey(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME)).isTrue();
    TypeDefinitionRegistry tdr = new SchemaParser()
        .parse(sdlFiles.get(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME));

    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    assertThat(tdr.types().keySet()).contains("AccountType");
  }

  @Test(expected = ServiceRegistrationException.class)
  public void loadThrowsExceptionOnInvalidSchemaResponseTest() {

    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTED_SCHEMA_INVALID));

    new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    ).sdlFiles();
  }

  @Test(expected = ServiceRegistrationException.class)
  public void requiresNonEmptyEndpointTest() {

    ServiceDefinition tServiceDefinition = ServiceDefinition.newBuilder().namespace("test-namespace").endpoint(" ")
        .type(Type.GRAPHQL).build();

    ServiceRegistration tServiceRegistration = ServiceRegistration.baseBuilder()
        .serviceDefinition(tServiceDefinition)
        .build();
    new IntrospectionServiceProvider(
        null,
        webClient,
        tServiceRegistration
    ).sdlFiles();

  }

  @Test(expected = ServiceRegistrationException.class)
  public void requiresNonNullEndpointTest() {

    ServiceDefinition tServiceDefinition = ServiceDefinition.newBuilder().namespace("test-namespace").endpoint(null)
        .type(Type.GRAPHQL).build();

    ServiceRegistration tServiceRegistration = ServiceRegistration.baseBuilder()
        .serviceDefinition(tServiceDefinition)
        .build();

    new IntrospectionServiceProvider(
        null,
        webClient,
        tServiceRegistration
    ).sdlFiles();

  }

  @Test(expected = ServiceRegistrationException.class)
  public void testRetryMaxAttempts() {

    MockResponse failedResponse = new MockResponse().setResponseCode(502);
    mockWebServer.enqueue(failedResponse);
    mockWebServer.enqueue(failedResponse);
    mockWebServer.enqueue(failedResponse);

    new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    );
  }

  @Test
  public void testRetrySucceeds() {
    MockResponse failedResponse = new MockResponse().setResponseCode(502);

    mockWebServer.enqueue(failedResponse);
    mockWebServer.enqueue(failedResponse);
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTED_SCHEMA));

    IntrospectionServiceProvider introspectionServiceProvider = new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    );

    Map<String, String> sdlFiles = introspectionServiceProvider.sdlFiles();
    assertThat(sdlFiles.containsKey(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME)).isTrue();
    TypeDefinitionRegistry tdr = new SchemaParser()
        .parse(sdlFiles.get(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME));

    assertThat(tdr.types().keySet()).contains("AccountType");
  }

  @Test(expected = ServiceRegistrationException.class)
  public void testRetryPropagatesError() {

    MockResponse failedResponse = new MockResponse().setResponseCode(404);
    mockWebServer.enqueue(failedResponse);

    new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    );
  }

  @Test
  public void loadDocumentRetainsDescriptions() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(INTROSPECTION_WITH_DESCRIPTION));

    IntrospectionServiceProvider introspectionServiceProvider = new IntrospectionServiceProvider(
        null,
        webClient,
        serviceRegistration
    );

    Map<String, String> sdlFiles = introspectionServiceProvider.sdlFiles();
    TypeDefinitionRegistry tdr = new SchemaParser()
        .parse(sdlFiles.get(IntrospectionServiceProvider.INTROSPECTION_FILE_NAME));

    ObjectTypeDefinition query = (ObjectTypeDefinition) tdr.types().get("Query");
    assertThat(query.getDescription().getContent())
        .isEqualTo("A simple GraphQL schema which is well described.\nAnd has multiple lines of description");

    for (FieldDefinition fd : query.getFieldDefinitions()) {
      if ("content".equals(fd.getName())) {
        assertThat(fd.getDescription().getContent()).contains("Multiline description for field on type extension");
      } else if ("translate".equals(fd.getName())) {
        assertThat(fd.getDescription().getContent()).contains("Multiline description for field definition.");

        InputValueDefinition fromLanguage = fd.getInputValueDefinitions().get(0);
        assertThat(fromLanguage.getDescription().getContent())
            .isEqualTo("Single line description for InputValueDefinition.");
      }
    }

    InputObjectTypeDefinition inputFoo = (InputObjectTypeDefinition) tdr.types().get("InputFoo");
    assertThat(inputFoo.getDescription().getContent())
        .contains("Multiline description for InputObjectTypeDefinition InputFoo");

    ObjectTypeDefinition foo = (ObjectTypeDefinition) tdr.types().get("Foo");
    assertThat(foo.getDescription().getContent()).isEqualTo("Single line description for ObjectTypeDefinition Foo");

    EnumTypeDefinition language = (EnumTypeDefinition) tdr.types().get("Language");
    assertThat(language.getDescription().getContent())
        .contains("Multiline description for EnumTypeDefinition language");

    for (EnumValueDefinition enumValueDefinition : language.getEnumValueDefinitions()) {
      if ("EN".equals(enumValueDefinition.getName())) {
        assertThat(enumValueDefinition.getDescription().getContent()).isEqualTo("English");
      } else if ("CH".equals(enumValueDefinition.getName())) {
        assertThat(enumValueDefinition.getDescription().getContent()).isEqualTo("Chinese");
      } else if ("FR".equals(enumValueDefinition.getName())) {
        assertThat(enumValueDefinition.getDescription().getContent()).isEqualTo("French");
      }
    }

    assertThat(tdr.getDirectiveDefinition("FooDirective").get().getDescription().getContent())
        .isEqualTo("Comments should work on directives.");

    //TODO: Test descriptions for Union, Interface, Scalars
  }

}
