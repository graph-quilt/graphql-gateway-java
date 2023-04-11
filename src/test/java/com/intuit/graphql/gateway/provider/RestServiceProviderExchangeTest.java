package com.intuit.graphql.gateway.provider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.exception.RestExecutionException;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.metrics.DownstreamCallEvent;
import com.intuit.graphql.gateway.metrics.ExecutionMetrics;
import com.intuit.graphql.gateway.registry.RestServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.s3.S3RegistrationCache;
import com.intuit.graphql.gateway.webclient.TxProvider;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.Scalars;
import graphql.execution.MergedField;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class RestServiceProviderExchangeTest {

  private static final Parser PARSER = new Parser();
  public static final String MOCK_SERVICE_RESPONSE = "{ \"name\" : \"The book\", "
      + "\"inventory\" : [ {\"CA\" : 100}, {\"AZ\" : 200}, {\"TX\" : 200} ] } }";
  public static final String TEST_BOOK_ID = "book-1";
  public static final String TEST_QUERY =
      "{ bookById(id : \"" + TEST_BOOK_ID + "\") { id name status price } }";
  public static final ExecutionInput TEST_EXECUTION_INPUT = ExecutionInput.newExecutionInput()
      .query(TEST_QUERY)
      .build();

  private static final int WIREMOCK_PORT = 4050;
  public static final String WIREMOCK_URL = "/books/" + TEST_BOOK_ID;

  private String testFlowFile;
  private GraphQLContext testGraphQLContext;

  private RestServiceProvider subject;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private ExecutionMetrics.ExecutionMetricsData mockExecutionMetricsData;

  @Before
  public void setup() throws IOException {
    testFlowFile = Resources
        .toString(Resources.getResource("rest-adapter/bookById.flow"), Charsets.UTF_8);

    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
        .arguments(new HashMap<String, Object>() {{
          put("id", TEST_BOOK_ID);
        }})
        .fieldDefinition(GraphQLFieldDefinition.newFieldDefinition()
            .name("bookById")
            .type(Scalars.GraphQLID) // just for testing
            .build())
        .operationDefinition(
            OperationDefinition.newOperationDefinition().name("query").operation(Operation.QUERY)
                .build())
        .mergedField(MergedField
            .newMergedField(Field
                .newField("bookById")
                .selectionSet(SelectionSet.newSelectionSet()
                    .selection(Field.newField("id").build())
                    .selection(Field.newField("name").build())
                    .selection(Field.newField("status").build())
                    .selection(Field.newField("price").build())
                    .build())
                .build())
            .build())
        .build();

    testGraphQLContext = GraphQLContext
        .newContext()
        .of(Document.class, PARSER.parseDocument(TEST_QUERY))
        .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()))
        .of(ServerRequest.class, MockServerRequest.builder()
            .header("user_channel", "bar")
            .build())
        .of(DataFetchingEnvironment.class, dfe)
        .of(ExecutionMetrics.ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();
  }

  @Test
  public void connectionRefusedTest() throws ExecutionException, InterruptedException {
    int invalidPort = 54321;
    // This expected exception is true prior to adding ExecutionMetrics features
    expectedEx.expect(ExecutionException.class);
    expectedEx.expectCause(instanceOf(RestExecutionException.class));
    expectedEx.expectMessage(containsString("Connection refused"));

    stubFor(any(urlPathMatching(WIREMOCK_URL))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(MOCK_SERVICE_RESPONSE)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    ServiceDefinition serviceDefinition = createTestServiceDefinition(invalidPort);
    ServiceRegistration serviceRegistration = createServiceRegistration(serviceDefinition,
        testFlowFile);
    subject = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());

    try {
      subject.query(TEST_EXECUTION_INPUT, testGraphQLContext).get();
    } finally {
      Mockito.verify(mockExecutionMetricsData, times(1))
          .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));
      verify(exactly(0), getRequestedFor(urlPathMatching(WIREMOCK_URL))
          .withHeader("accept", containing("application/json"))
      );
    }
  }

  @Test
  public void connectionTimeoutTest() throws ExecutionException, InterruptedException {
    // This expected exception is true prior to adding ExecutionMetrics features
    expectedEx.expect(ExecutionException.class);
    expectedEx.expectCause(instanceOf(RestExecutionException.class));
    expectedEx.expectMessage(containsString(
        "Did not observe any item or terminal signal within 2500ms"));

    stubFor(any(urlPathMatching(WIREMOCK_URL))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withFixedDelay(3000)
            .withBody(MOCK_SERVICE_RESPONSE)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    ServiceDefinition serviceDefinition = createTestServiceDefinition(WIREMOCK_PORT);
    ServiceRegistration serviceRegistration = createServiceRegistration(serviceDefinition,
        testFlowFile);
    subject = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    try {
      subject.query(TEST_EXECUTION_INPUT, testGraphQLContext).get();
    } finally {
      Mockito.verify(mockExecutionMetricsData, times(1))
          .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

      verify(exactly(1), getRequestedFor(urlPathMatching(WIREMOCK_URL))
          .withHeader("accept", containing("application/json"))
      );
    }
  }

  @Test
  public void httpStatus504ResponseTest() throws ExecutionException, InterruptedException {
    // This expected exception is true prior to adding ExecutionMetrics features
    expectedEx.expect(ExecutionException.class);
    expectedEx.expectCause(instanceOf(RestExecutionException.class));
    expectedEx.expectMessage(
        containsString("Server error encountered.  statusCode=504 responseBody="));

    stubFor(any(urlPathMatching(WIREMOCK_URL))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(504)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    ServiceDefinition serviceDefinition = createTestServiceDefinition(WIREMOCK_PORT);
    ServiceRegistration serviceRegistration = createServiceRegistration(serviceDefinition,
        testFlowFile);
    subject = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    try {
      subject.query(TEST_EXECUTION_INPUT, testGraphQLContext).get();
    } finally {
      Mockito.verify(mockExecutionMetricsData, times(1))
          .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

      verify(exactly(1), getRequestedFor(urlPathMatching(WIREMOCK_URL))
          .withHeader("accept", containing("application/json"))
      );
    }
  }

  @Test
  public void httpStatus401ResponseTest() throws ExecutionException, InterruptedException {
    // This expected exception is true prior to adding ExecutionMetrics features
    expectedEx.expect(ExecutionException.class);
    expectedEx.expectCause(instanceOf(RestExecutionException.class));
    expectedEx.expectMessage(
        containsString("Server error encountered.  statusCode=401 responseBody="));

    stubFor(any(urlPathMatching(WIREMOCK_URL))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(401)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    ServiceDefinition serviceDefinition = createTestServiceDefinition(WIREMOCK_PORT);
    ServiceRegistration serviceRegistration = createServiceRegistration(serviceDefinition,
        testFlowFile);
    subject = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    try {
      subject.query(TEST_EXECUTION_INPUT, testGraphQLContext).get();
    } finally {
      Mockito.verify(mockExecutionMetricsData, times(1))
          .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));
      verify(exactly(1), getRequestedFor(urlPathMatching(WIREMOCK_URL))
          .withHeader("accept", containing("application/json"))
      );
    }
  }

  @Test
  public void InvalidResponseBodyTest() throws ExecutionException, InterruptedException {
    // This expected exception is true prior to adding ExecutionMetrics features
    expectedEx.expect(ExecutionException.class);
    expectedEx.expectCause(instanceOf(RestExecutionException.class));
    expectedEx.expectMessage(containsString("Error processing json response."));

    stubFor(any(urlPathMatching(WIREMOCK_URL))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("This is not valid.  Json is expected")
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    ServiceDefinition serviceDefinition = createTestServiceDefinition(WIREMOCK_PORT);
    ServiceRegistration serviceRegistration = createServiceRegistration(serviceDefinition,
        testFlowFile);
    subject = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    try {
      subject.query(TEST_EXECUTION_INPUT, testGraphQLContext).get();
    } finally {
      Mockito.verify(mockExecutionMetricsData, times(1))
          .addDownstreamCallEvent(ArgumentMatchers.any(
              DownstreamCallEvent.class));
      verify(exactly(1), getRequestedFor(urlPathMatching(WIREMOCK_URL))
          .withHeader("accept", containing("application/json"))
      );
    }
  }

  private ServiceDefinition createTestServiceDefinition(int port) {
    return ServiceDefinition.newBuilder()
        .namespace("RestServiceProviderTest")
        .endpoint("http://localhost:" + port)
        .timeout(2500)
        .appId("RestServiceProviderTest")
        .type(ServiceDefinition.Type.REST).build();
  }

  private ServiceRegistration createServiceRegistration(ServiceDefinition serviceDefinition,
      String flowResource) {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(
        serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", flowResource);
    return s3RegistrationCache.toServiceRegistration();
  }

}
