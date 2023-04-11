package com.intuit.graphql.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.intuit.graphql.gateway.logging.ContextFactory.HEADER_APPID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.GraphqlGatewayApplication;
import com.intuit.graphql.gateway.beans.TestBeans;
import com.intuit.graphql.gateway.config.properties.RegistrationBreakingChangeProperties;
import com.intuit.graphql.gateway.graphql.GraphQLExecutor;
import com.intuit.graphql.gateway.graphql.SchemaManager;
import com.intuit.graphql.gateway.registry.ServiceRegistrationS3PathResolver;
import com.intuit.graphql.gateway.s3.FilePathResolver;
import com.intuit.graphql.gateway.s3.ImmutableFilePathResolver;
import com.intuit.graphql.gateway.s3.S3Configuration;
import com.intuit.graphql.gateway.s3.S3Configuration.S3Upload;
import com.intuit.graphql.orchestrator.GraphQLOrchestrator;
import graphql.ExecutionInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("UnstableApiUsage")
@SpringBootTest(classes = {GraphqlGatewayApplication.class,
    TestServiceRegistry.class, TestBeans.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableConfigurationProperties
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {"app.env=test", "spring.profiles.active=test"})
@ActiveProfiles("test")
public class GraphqlGatewayTests extends IntegrationTestCase {

  private static String simpleQueryRequest;
  private static String simpleQueryResponse;

  private static String complexQueryRequest;
  private static String complexQueryResponse;

  private static String multipleTopLevelFieldsRequest;
  private static String multipleTopLevelFieldsResponse;

  private static String booksIntrospectionResponse;
  private static String whitelistQueryRequest;
  private static String whitelistQueryResponse;

  @Autowired
  private S3Configuration s3Configuration;

  static {
    try {
      simpleQueryRequest = Resources
          .toString(Resources.getResource("mocks/graphql/requests/simplequery.json"), Charsets.UTF_8);
      simpleQueryResponse = Resources
          .toString(Resources.getResource("mocks/graphql/responses/simplequery.json"), Charsets.UTF_8);

      complexQueryRequest = Resources
          .toString(Resources.getResource("mocks/graphql/requests/complexquery.json"), Charsets.UTF_8);
      complexQueryResponse = Resources
          .toString(Resources.getResource("mocks/graphql/responses/complexquery.json"), Charsets.UTF_8);

      multipleTopLevelFieldsRequest = Resources
          .toString(Resources.getResource("mocks/graphql/requests/multiple-top-level-fields.json"), Charsets.UTF_8);
      multipleTopLevelFieldsResponse = Resources
          .toString(Resources.getResource("mocks/graphql/responses/multiple-top-level-fields.json"), Charsets.UTF_8);

      booksIntrospectionResponse = Resources
          .toString(Resources.getResource("mocks/graphql/responses/books-introspection-resp.json"), Charsets.UTF_8);

      whitelistQueryRequest = Resources
          .toString(Resources.getResource("mocks/graphql/requests/whitelistquery.json"), Charsets.UTF_8);
      whitelistQueryResponse = Resources
          .toString(Resources.getResource("mocks/graphql/responses/whitelistquery.json"), Charsets.UTF_8);
    } catch (IOException ignored) {
      assertThat(true).isFalse();
    }
  }

  @MockBean
  private GraphQLExecutor<ExecutionInput> graphQLExecutor;

  //prevents auto-initialization of schema manager that fails tests
  @MockBean
  private SchemaManager schemaManager;

  @Autowired
  private ResourceLoader resourceLoader;

  @MockBean
  private RegistrationBreakingChangeProperties registrationBreakingChangeProperties;

  // qa and foo are configured as env and appName in application-test.yml
  FilePathResolver s3PathResolver = ImmutableFilePathResolver.of("qa", "1.0.0", "foo");
  ServiceRegistrationS3PathResolver serviceRegistrationS3PathResolver = new ServiceRegistrationS3PathResolver(
      s3PathResolver);

  private String baseUrl;

  private String healthUrl;
  private String graphiqlUrl;
  private String visualizerUrl;
  private String graphqlUrl;

  @Before
  @Override
  public void initialize() {
    super.initialize();

    baseUrl = String.format("http://localhost:%d", port);
    healthUrl = String.format("%s/health/local", baseUrl);
    graphiqlUrl = String.format("%s/graphiql", baseUrl);
    visualizerUrl = String.format("%s/graphiql/visualizer", baseUrl);
    graphqlUrl = String.format("%s/graphql", baseUrl);

    GraphQLOrchestrator graphQLOrchestrator = GraphQLOrchestratorTestUtil.getGraphQLOrchestrator();
    when(graphQLExecutor.execute(Mockito.any()))
        .thenAnswer(answer -> graphQLOrchestrator.execute(answer.<ExecutionInput>getArgument(0)));
    s3Configuration.setUpload(Mockito.spy(new S3Upload()));

    when(registrationBreakingChangeProperties.isEnabled()).thenReturn(true);
  }

  @Test
  public void canPingLocalhostTest() {
    HttpEntity<String> entity = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.exchange(healthUrl, HttpMethod.GET, entity, String.class);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Health Check OK");
  }

  @Test
  public void canLoadGraphiQLTest() throws IOException {
    String graphiQLContent = IOUtils
        .toString(this.resourceLoader.getResource("classpath:graphiql.html").getInputStream(), StandardCharsets.UTF_8);

    HttpEntity<String> entity = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.exchange(graphiqlUrl, HttpMethod.GET, entity, String.class);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(graphiQLContent);
  }

  @Test
  public void canLoadVisualizerQLTest() throws IOException {
    String visualizerContent = IOUtils
        .toString(this.resourceLoader.getResource("classpath:visualizer.html").getInputStream(),
            StandardCharsets.UTF_8);
    HttpEntity<String> entity = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.exchange(visualizerUrl, HttpMethod.GET, entity, String.class);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(visualizerContent);
  }

  @Test
  public void canMakeSimpleGraphQLQueryTest() {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(simpleQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(simpleQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    HttpEntity<String> entity = new HttpEntity<>(simpleQueryRequest, headers);
    ResponseEntity<String> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();

    verify(postRequestedFor(urlPathMatching("/dos/graphql")));
    verify(0, postRequestedFor(urlPathMatching("/v4os/graphql")));
  }

  @Test
  public void canMakeComplexGraphQLQueryTest() throws JSONException {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    HttpEntity<String> entity = new HttpEntity<>(complexQueryRequest, headers);
    ResponseEntity<String> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
    assertEquals(complexQueryResponse, response.getBody(), JSONCompareMode.STRICT);

    verify(postRequestedFor(urlPathMatching("/dos/graphql")));
    verify(postRequestedFor(urlPathMatching("/v4os/graphql")));
  }

  @Test
  public void nonWhitelistedClientCannotMakeQueryTest() throws JSONException {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse) //using complexQueryResponse is intentional
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse) //using complexQueryResponse is intentional
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/cgcs/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\n" + "  \"data\": { \"cgcs\": \"someValue\" }}")
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    HttpEntity<String> entity = new HttpEntity<>(whitelistQueryRequest, headers);
    ResponseEntity<String> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
    assertThat(response.getBody()).contains("Exception while fetching data (/cgcs)", "DataRetrieverException");
    //use of complexQueryResponse is intentional here
    assertEquals(complexQueryResponse, response.getBody(), JSONCompareMode.LENIENT);

    verify(postRequestedFor(urlPathMatching("/dos/graphql")));
    verify(postRequestedFor(urlPathMatching("/v4os/graphql")));
    verify(exactly(0), postRequestedFor(urlPathMatching("/cgcs/graphql")));
  }

  @Test
  public void onlyWhitelistedClientCanMakeQueryTest() throws JSONException {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse) //using complexQueryResponse is intentional
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse) //using complexQueryResponse is intentional
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/cgcs/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\n" + "  \"data\": { \"cgcs\": \"someValue\" }}")
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    headers.add(HEADER_APPID, "test.client");
    HttpEntity<String> entity = new HttpEntity<>(whitelistQueryRequest, headers);
    ResponseEntity<String> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
    assertThat(response.getBody())
        .doesNotContain("errors", "Exception while fetching data (/cgcs)", "DataRetrieverException");
    assertEquals(whitelistQueryResponse, response.getBody(), JSONCompareMode.STRICT);

    verify(postRequestedFor(urlPathMatching("/dos/graphql")));
    verify(postRequestedFor(urlPathMatching("/v4os/graphql")));
    verify(postRequestedFor(urlPathMatching("/cgcs/graphql")));
  }

  @Test
  public void makesOneRequestPerProviderTest() {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(multipleTopLevelFieldsResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(multipleTopLevelFieldsResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    HttpEntity<String> entity = new HttpEntity<>(multipleTopLevelFieldsRequest, headers);
    ResponseEntity<String> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();

    verify(1, postRequestedFor(urlPathMatching("/dos/graphql")));
    verify(1, postRequestedFor(urlPathMatching("/v4os/graphql")));
  }

  @Test
  public void readTimeOutOnProviderResponseDelayTest() throws JSONException {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withFixedDelay(4001)
            .withStatus(200)
            .withBody(complexQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    HttpEntity<String> entity = new HttpEntity<>(complexQueryRequest, headers);
    ResponseEntity<String> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotEmpty();
    assertEquals("{\"data\":{\"dos\":null,\"v4os\":{\"identity\":{\"personalInfo\":{\"currentFamilyName\":\"\"}}}}}",
        response.getBody(), JSONCompareMode.LENIENT);

    verify(postRequestedFor(urlPathMatching("/v4os/graphql")));
  }

  @Test
  public void canForwardWhitelistedHeadersTest() {

    stubFor(any(urlPathMatching("/dos/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));
    stubFor(any(urlPathMatching("/v4os/graphql"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(complexQueryResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    headers.add("bar", "bar-test");

    HttpEntity<String> entity = new HttpEntity<>(complexQueryRequest, headers);
    restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, String.class);

    verify(postRequestedFor(urlPathMatching("/dos/graphql")).withHeader("bar", containing("bar-test")));
    verify(postRequestedFor(urlPathMatching("/v4os/graphql")).withoutHeader("bar"));
  }

}
