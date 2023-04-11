package com.intuit.graphql.gateway.integration;

import static com.google.common.io.Resources.getResource;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.webclient.TxProvider;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Rule;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

public class GraphQLTestCase {

  /**
   * Single instance graphql executor for all tests.
   */
  @Rule
  public GraphQLExecutorRule graphQLExecutorRule = new GraphQLExecutorRule();

  TestResult runTestQuery(final TestQuery testQuery) {
    return new TestResult(graphQLExecutorRule.execute(testQuery.asExecutionInput())
        .join());
  }

  String getQuery(final String filename) {
    try {
      return Resources.toString(getResource("integration/queries/" + filename), Charset.defaultCharset());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  String getResponse(final String filename) {
    try {
      return Resources.toString(getResource("integration/responses/" + filename), Charset.defaultCharset());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void addGraphQLResponse(StubMapping stubMapping) {
    graphQLExecutorRule.wireMockRule.addStubMapping(stubMapping);
  }

  protected static class TestResult {

    private final ExecutionResult executionResult;

    private TestResult(ExecutionResult executionResult) {

      this.executionResult = executionResult;
    }

    public void thenAssert(Consumer<ExecutionResult> executionResultConsumer) {
      executionResultConsumer.accept(executionResult);
    }
  }

  protected static class TestQuery {

    public String query;

    public String operationName;

    public Map<String, Object> variables;

    private TestQuery(final Builder builder) {
      query = builder.query;
      operationName = builder.operationName;
      variables = builder.variables;
    }

    public static Builder newQuery() {
      return new Builder();
    }

    public ExecutionInput asExecutionInput() {
      return ExecutionInput.newExecutionInput()
          .query(query)
          .variables(this.variables)
          .operationName(this.operationName)
          .context(GraphQLContext.newContext()
              .of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx()))
              .of(ServerRequest.class, MockServerRequest.builder().build()))
          .build();
    }

    public static final class Builder {

      private String query;
      private String operationName;
      private Map<String, Object> variables = new HashMap<>();

      private Builder() {
      }

      public Builder query(final String val) {
        query = val;
        return this;
      }

      public Builder operationName(final String val) {
        operationName = val;
        return this;
      }

      public Builder variables(final Map<String, Object> val) {
        variables = val;
        return this;
      }

      public TestQuery build() {
        return new TestQuery(this);
      }
    }
  }
}
