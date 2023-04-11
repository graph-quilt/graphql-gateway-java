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
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
import com.intuit.service.dsl.evaluator.exceptions.ServiceEvaluatorException;
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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class RestServiceProviderTest {

  private String bookByIdFlow;

  private String bookByPublisherAndYearFlow;

  private String addBookFlowDefaultBody;

  private String addBookFlowCustomBody;

  private String addBookDefaultBodyMultipleArgumentFlow;

  private ServiceDefinition serviceDefinition;

  private static final int WIREMOCK_PORT = 4050;
  private static final Parser parser = new Parser();

  @Mock
  private ExecutionMetrics.ExecutionMetricsData mockExecutionMetricsData;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

  @Before
  public void setup() throws IOException {

    bookByIdFlow = Resources
        .toString(Resources.getResource("rest-adapter/bookById.flow"), Charsets.UTF_8);
    bookByPublisherAndYearFlow = Resources
            .toString(Resources.getResource("rest-adapter/bookByPublisherAndYear.flow"), Charsets.UTF_8);
    addBookFlowDefaultBody = Resources
        .toString(Resources.getResource("rest-adapter/addBookDefaultBody.flow"), Charsets.UTF_8);
    addBookFlowCustomBody = Resources
        .toString(Resources.getResource("rest-adapter/addBookCustomBody.flow"), Charsets.UTF_8);
    addBookDefaultBodyMultipleArgumentFlow  = Resources
        .toString(Resources.getResource("rest-adapter/addBookDefaultBodyMultipleArgument.flow"), Charsets.UTF_8);

    serviceDefinition = ServiceDefinition.newBuilder()
        .namespace("RestServiceProviderTest")
        .endpoint("http://localhost:" + WIREMOCK_PORT)
        .timeout(2500L)
        .appId("RestServiceProviderTest")
        .type(ServiceDefinition.Type.REST).build();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void canExecuteQueryToGet() throws ExecutionException, InterruptedException {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", bookByIdFlow);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());

    String mockServiceResponse = "{ \"name\" : \"The book\", \"inventory\" : [ {\"CA\" : 100}, {\"AZ\" : 200}, {\"TX\" : 200} ] } }";
    String id = "book-1";
    String wiremockUrl = "/books/" + id;

    stubFor(any(urlPathMatching(wiremockUrl))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(mockServiceResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    String query = "{ bookById(id : \"" + id + "\") { id name status price } }";

    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(query)
        .build();

    ServerRequest serverRequest = MockServerRequest.builder()
        .header("user_channel", "bar")
        .build();

    SelectionSet selectionSet = SelectionSet.newSelectionSet()
        .selection(Field.newField("id").build())
        .selection(Field.newField("name").build())
        .selection(Field.newField("status").build())
        .selection(Field.newField("price").build()).build();

    GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
        .name("bookById")
        .type(Scalars.GraphQLID) // just for testing
        .build();

    Field topLevelField = Field.newField("bookById").selectionSet(selectionSet).build();

    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
        .arguments(new HashMap<String, Object>() {{
          put("id", id);
        }})
        .fieldDefinition(fieldDefinition)
        .operationDefinition(
            OperationDefinition.newOperationDefinition().name("query").operation(Operation.QUERY).build())
        .mergedField(MergedField.newMergedField(topLevelField).build())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext()
        .of(Document.class, parser.parseDocument(query))
        .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()),
            ServerRequest.class, serverRequest)
        .of(DataFetchingEnvironment.class, dfe)
        .of(ExecutionMetrics.ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    Map<String, Object> response = restSvcProvider.query(executionInput, graphQLContext).get();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

    verify(exactly(1), getRequestedFor(urlPathMatching(wiremockUrl))
        .withHeader("accept", containing("application/json"))
    );
    assertThat(response).containsOnlyKeys("data");

    Map<String, Object> data = (Map<String, Object>) response.get("data");
    assertThat(data).containsOnlyKeys("bookById");

    Map<String, Object> bookById = (Map<String, Object>) data.get("bookById");
    assertThat(bookById).containsOnlyKeys("name", "inventory");

    String name = (String) bookById.get("name");
    assertThat(name).isEqualTo("The book");

    List<Map<String, Integer>> inventory = (List<Map<String, Integer>>) bookById.get("inventory");
    assertThat(inventory).hasSize(3);

  }

  @Test
  public void canExecuteQueryWithMultipleArguments() throws ExecutionException, InterruptedException {
    RestServiceProvider restSvcProvider = createRestServiceProvider(serviceDefinition, bookByPublisherAndYearFlow);

    String publisherId = "publisher1";
    String yearPublished = "2020";
    String wiremockUrl = "/books";

    String query = String.format("{ bookByPublisherAndYear(publisherId: %s, yearPublished: %s) { title price } }",
            publisherId, yearPublished);
    String mockDownstreamServiceResponse = "{ \"title\" : \"The book\", \"price\" : 100 } }";

    stubFor(any(urlPathMatching(wiremockUrl))
            .withHeader("accept", containing("application/json"))
            .withQueryParam("publisherId", containing(publisherId))
            .withQueryParam("yearPublished", containing(yearPublished))
            .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(mockDownstreamServiceResponse)
                    .withHeader("Content-Type", "application/json;charset=UTF-8")));

    ExecutionInput executionInput = createExecutionInput(query);

    DataFetchingEnvironment dfe = createDataFetchingEnvironment(
            "bookByPublisherAndYear",
            Arrays.asList("title", "price"),
            new HashMap<String, Object>() {{
              put("publisherId", publisherId);
              put("yearPublished", yearPublished);
            }}
    );

    ServerRequest serverRequest = MockServerRequest.builder()
            .header("user_channel", "bar")
            .build();

    GraphQLContext graphQLContext = createGraphQLContext(query, serverRequest, dfe);

    Map<String, Object> response = restSvcProvider.query(executionInput, graphQLContext).get();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

    verify(exactly(1), getRequestedFor(urlPathMatching(wiremockUrl)));
    assertThat(response).containsOnlyKeys("data");

    Map<String, Object> data = (Map<String, Object>) response.get("data");
    assertThat(data).containsOnlyKeys("bookByPublisherAndYear");

    Map<String, Object> bookById = (Map<String, Object>) data.get("bookByPublisherAndYear");
    assertThat(bookById).containsOnlyKeys("title", "price");
  }

  @Test
  public void canExecuteInsertMutationToPostWithDefaultBody() throws ExecutionException, InterruptedException {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", addBookFlowDefaultBody);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());

    String mockServiceResponse = "{\"id\": \"book-1\",\"name\": \"The Book\",\"price\": 100}";
    String wiremockUrl = "/books";
    stubFor(any(urlEqualTo(wiremockUrl))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(mockServiceResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    Map<String, Object> bookDetailsMap = new HashMap<>();
    bookDetailsMap.put("id", "book-1");
    bookDetailsMap.put("name", "The Book");
    bookDetailsMap.put("price", BigInteger.valueOf(100));

    String query = "mutation AddBook ($newBookvar : NewBookInput!){ addBook(newBook : $newBookvar) { id name } }";
    Map<String, Object> queryVar = new HashMap<>();
    queryVar.put("newBookvar", bookDetailsMap);

    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(query)
        .variables(queryVar)
        .build();

    ServerRequest serverRequest = MockServerRequest.builder()
        .header("user_channel", "bar")
        .build();

    SelectionSet selectionSet = SelectionSet.newSelectionSet()
        .selection(Field.newField("id").build())
        .selection(Field.newField("name").build()).build();

    Field topLevelField = Field.newField("addBook").selectionSet(selectionSet).build();

    GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
        .name("addBook")
        .type(Scalars.GraphQLID) // just for testing
        .build();

    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
        .fieldDefinition(fieldDefinition)
        .arguments(new HashMap<String, Object>() {{
          put("newBook", bookDetailsMap);
        }})
        .operationDefinition(
            OperationDefinition.newOperationDefinition().name("mutation").operation(Operation.MUTATION).build())
        .variables(queryVar)
        .mergedField(MergedField.newMergedField(topLevelField).build())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext()
        .of(Document.class, parser.parseDocument(query))
        .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()),
            ServerRequest.class, serverRequest)
        .of(DataFetchingEnvironment.class, dfe)
        .of(ExecutionMetrics.ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    Map<String, Object> response = restSvcProvider.query(executionInput, graphQLContext).get();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

    verify(exactly(1), postRequestedFor(urlEqualTo(wiremockUrl))
        .withRequestBody(equalToJson(mockServiceResponse))
    );
    assertThat(response).containsOnlyKeys("data");

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");
    assertThat(data).containsOnlyKeys("addBook");

    @SuppressWarnings("unchecked")
    Map<String, Object> addBook = (Map<String, Object>) data.get("addBook");
    assertThat(addBook).containsOnlyKeys("name", "id", "price");

    String name = (String) addBook.get("name");
    assertThat(name).isEqualTo("The Book");

    String id = (String) addBook.get("id");
    assertThat(id).isEqualTo("book-1");
  }

  @Test
  public void canExecuteInsertMutationToPostWithDefaultBodyWithoutVariable() throws ExecutionException, InterruptedException {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", addBookFlowDefaultBody);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());

    String mockServiceResponse = "{\"id\": \"book-1\",\"name\": \"The Book\",\"price\": 100}";
    String wiremockUrl = "/books";
    stubFor(any(urlEqualTo(wiremockUrl))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(mockServiceResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    Map<String, Object> bookDetailsMap = new HashMap<>();
    bookDetailsMap.put("id", "book-1");
    bookDetailsMap.put("name", "The Book");
    bookDetailsMap.put("price", BigInteger.valueOf(100));

    String query = "mutation AddBook { addBook(newBook : {id: \"book-1\", name: \"The book\", price: 100}) { id name } }";

    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(query)
        .build();

    ServerRequest serverRequest = MockServerRequest.builder()
        .header("user_channel", "bar")
        .build();

    SelectionSet selectionSet = SelectionSet.newSelectionSet()
        .selection(Field.newField("id").build())
        .selection(Field.newField("name").build()).build();

    Field topLevelField = Field.newField("addBook").selectionSet(selectionSet).build();
    GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
        .name("addBook")
        .type(Scalars.GraphQLID) // just for testing
        .build();
    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
        .fieldDefinition(fieldDefinition)
        .arguments(new HashMap<String, Object>() {{
          put("newBook", bookDetailsMap);
        }})
        .operationDefinition(
            OperationDefinition.newOperationDefinition().name("mutation").operation(Operation.MUTATION).build())
        .mergedField(MergedField.newMergedField(topLevelField).build())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext()
        .of(Document.class, parser.parseDocument(query))
        .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()),
            ServerRequest.class, serverRequest)
        .of(DataFetchingEnvironment.class, dfe)
        .of(ExecutionMetrics.ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    Map<String, Object> response = restSvcProvider.query(executionInput, graphQLContext).get();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

    verify(exactly(1), postRequestedFor(urlEqualTo(wiremockUrl))
        .withRequestBody(equalToJson(mockServiceResponse))
    );
    assertThat(response).containsOnlyKeys("data");

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");
    assertThat(data).containsOnlyKeys("addBook");

    @SuppressWarnings("unchecked")
    Map<String, Object> addBook = (Map<String, Object>) data.get("addBook");
    assertThat(addBook).containsOnlyKeys("name", "id", "price");

    String name = (String) addBook.get("name");
    assertThat(name).isEqualTo("The Book");

    String id = (String) addBook.get("id");
    assertThat(id).isEqualTo("book-1");
  }

  @Test
  public void canExecuteInsertMutationToPostWithCustomBody() throws ExecutionException, InterruptedException {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", addBookFlowCustomBody);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());

    String mockServiceResponse = "{\"id\": \"book-1\",\"name\": \"The Book\",\"price\": 100}";
    String wiremockUrl = "/books";
    stubFor(any(urlEqualTo(wiremockUrl))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(mockServiceResponse)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    Map<String, Object> bookDetailsMap = new HashMap<>();
    bookDetailsMap.put("id", "book-1");
    bookDetailsMap.put("name", "The Book");
    bookDetailsMap.put("price", BigInteger.valueOf(100));

    String query = "mutation AddBook ($newBookvar : NewBookInput!){ addBook(newBook : $newBookvar) { id name } }";
    Map<String, Object> queryVar = new HashMap<>();
    queryVar.put("newBookvar", bookDetailsMap);

    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(query)
        .variables(queryVar)
        .build();

    ServerRequest serverRequest = MockServerRequest.builder()
        .header("user_channel", "bar")
        .build();

    SelectionSet selectionSet = SelectionSet.newSelectionSet()
        .selection(Field.newField("id").build())
        .selection(Field.newField("name").build()).build();

    Field topLevelField = Field.newField("addBook").selectionSet(selectionSet).build();
    GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
        .name("addBook")
        .type(Scalars.GraphQLID) // just for testing
        .build();
    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
        .fieldDefinition(fieldDefinition)
        .arguments(new HashMap<String, Object>() {{
          put("newBook", bookDetailsMap);
        }})
        .operationDefinition(
            OperationDefinition.newOperationDefinition().name("mutation").operation(Operation.MUTATION).build())
        .variables(queryVar)
        .mergedField(MergedField.newMergedField(topLevelField).build())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext()
        .of(Document.class, parser.parseDocument(query))
        .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()),
            ServerRequest.class, serverRequest)
        .of(DataFetchingEnvironment.class, dfe)
        .of(ExecutionMetrics.ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    Map<String, Object> response = restSvcProvider.query(executionInput, graphQLContext).get();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));

    verify(exactly(1), postRequestedFor(urlEqualTo(wiremockUrl))
        .withRequestBody(equalToJson(mockServiceResponse))
    );

    assertThat(response).containsOnlyKeys("data");

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");
    assertThat(data).containsOnlyKeys("addBook");

    @SuppressWarnings("unchecked")
    Map<String, Object> addBook = (Map<String, Object>) data.get("addBook");
    assertThat(addBook).containsOnlyKeys("name", "id", "price");

    String name = (String) addBook.get("name");
    assertThat(name).isEqualTo("The Book");

    String id = (String) addBook.get("id");
    assertThat(id).isEqualTo("book-1");
  }

  @Test(expected = ServiceEvaluatorException.class)
  public void executeFailedMutationWithMultipleArgumentsServiceEvaluatorWithDefaultBody() throws ExecutionException, InterruptedException {
    RestServiceProvider restServiceProvider = createRestServiceProvider(serviceDefinition,
            addBookDefaultBodyMultipleArgumentFlow);

    String title = "The Book";
    String price = "100";

    String query = String.format("mutation { addBook(title: \"%s\", price: \"%s\") { title price } }", title, price);

    ExecutionInput executionInput = createExecutionInput(query);

    DataFetchingEnvironment dfe = createDataFetchingEnvironment(
            "bookByPublisherAndYear",
            Arrays.asList("title", "price"),
            new HashMap<String, Object>() {{
              put("title", title);
              put("price", price);
            }}
    );

    ServerRequest serverRequest = MockServerRequest.builder()
            .header("user_channel", "bar")
            .build();

    GraphQLContext graphQLContext = createGraphQLContext(query, serverRequest, dfe);

    restServiceProvider.query(executionInput, graphQLContext).get();
  }

  @Test
  public void canGetNamespace() {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", addBookFlowDefaultBody);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    assertThat(restSvcProvider.getNameSpace()).isEqualTo("RestServiceProviderTest");
  }

  @Test
  public void serviceTypeIsRest() {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", addBookFlowDefaultBody);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    assertThat(restSvcProvider.getSeviceType()).isEqualTo(ServiceType.REST);
  }

  @Test
  public void canGetSDLFiles() {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", addBookFlowDefaultBody);
    s3RegistrationCache.addGraphqlResource("main/graphql/schema.graphqls", "type Query {} ");
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    ServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());
    assertThat(restSvcProvider.sdlFiles()).containsOnlyKeys("main/graphql/schema.graphqls");
  }


  private RestServiceProvider createRestServiceProvider(ServiceDefinition serviceDefinition,
                                                        String flowDSL) {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", flowDSL);
    RestServiceRegistration restServiceRegistration = (RestServiceRegistration) s3RegistrationCache.toServiceRegistration();

    WebClient webClient = WebClient.builder().build();
    return new RestServiceProvider(restServiceRegistration, webClient);
  }

  private ExecutionInput createExecutionInput(String query) {
    return ExecutionInput.newExecutionInput()
            .query(query)
            .build();
  }

  private GraphQLContext createGraphQLContext(String query, ServerRequest serverRequest, DataFetchingEnvironment dfe) {
    return GraphQLContext.newContext()
      .of(Document.class, parser.parseDocument(query))
      .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()), ServerRequest.class, serverRequest)
      .of(DataFetchingEnvironment.class, dfe)
      .of(ExecutionMetrics.ExecutionMetricsData.class, mockExecutionMetricsData)
      .build();
  }

  private DataFetchingEnvironment createDataFetchingEnvironment(String fieldName,
                                                                List<String> subSelectionFields,
                                                                Map<String, Object> arguments
                                                                ) {
    SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();
    subSelectionFields.stream().forEach(s -> {
      selectionSetBuilder.selection(Field.newField(s).build());
    });
    SelectionSet selectionSet = selectionSetBuilder.build();

    GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
            .name(fieldName)
            .type(Scalars.GraphQLID) // just for testing
            .build();

    Field topLevelField = Field.newField(fieldName).selectionSet(selectionSet).build();

    OperationDefinition operationDefinition = OperationDefinition
            .newOperationDefinition()
            .name("query")
            .operation(Operation.QUERY)
            .build();

    return newDataFetchingEnvironment()
            .arguments(arguments)
            .fieldDefinition(fieldDefinition)
            .operationDefinition(operationDefinition)
            .mergedField(MergedField.newMergedField(topLevelField).build())
            .build();
  }

  @Test
  public void canHandle3xxExceptionOnExecuteQuery() {
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(serviceDefinition);
    s3RegistrationCache.addFlowResource("main/flow/service.flow", bookByIdFlow);
    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();

    RestServiceProvider restSvcProvider = new RestServiceProvider((RestServiceRegistration) serviceRegistration,
        WebClient.builder().build());

    String id = "book-1";
    String wiremockUrl = "/books/" + id;

    stubFor(any(urlPathMatching(wiremockUrl))
        .withHeader("accept", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(302)
            .withHeader("Content-Type", "application/json;charset=UTF-8")));

    String query = "{ bookById(id : \"" + id + "\") { id name status price } }";

    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(query)
        .build();

    ServerRequest serverRequest = MockServerRequest.builder()
        .header("user_channel", "bar")
        .build();

    SelectionSet selectionSet = SelectionSet.newSelectionSet()
        .selection(Field.newField("id").build())
        .selection(Field.newField("name").build())
        .selection(Field.newField("status").build())
        .selection(Field.newField("price").build()).build();

    GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
        .name("bookById")
        .type(Scalars.GraphQLID) // just for testing
        .build();

    Field topLevelField = Field.newField("bookById").selectionSet(selectionSet).build();

    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
        .arguments(new HashMap<String, Object>() {{
          put("id", id);
        }})
        .fieldDefinition(fieldDefinition)
        .operationDefinition(
            OperationDefinition.newOperationDefinition().name("query").operation(Operation.QUERY).build())
        .mergedField(MergedField.newMergedField(topLevelField).build())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext()
        .of(Document.class, parser.parseDocument(query))
        .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()),
            ServerRequest.class, serverRequest)
        .of(DataFetchingEnvironment.class, dfe)
        .build();

    assertThatThrownBy(() -> restSvcProvider.query(executionInput, graphQLContext).get())
        .hasCauseInstanceOf(RestExecutionException.class)
        .hasStackTraceContaining("302");
  }

}
