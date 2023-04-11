package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.introspection.IntrospectionManager;
import com.intuit.graphql.gateway.metrics.ExecutionMetricsManager;
import com.intuit.graphql.orchestrator.GraphQLOrchestrator;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandler;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GraphqlGatewayExecutor implements GraphQLExecutor<ExecutionInput> {

  private final SchemaManager schemaManager;
  private final DataFetcherExceptionHandler dataFetcherExceptionHandler;
  private final AuthZManager authZManager;
  private final IntrospectionManager introspectionManager;
  private final ExecutionMetricsManager executionMetricsManager;

  public GraphqlGatewayExecutor(final SchemaManager schemaManager, final AuthZManager authZManager,
      final IntrospectionManager introspectionManager,
      final ExecutionMetricsManager executionMetricsManager, final DataFetcherExceptionHandler dataFetcherExceptionHandler) {
    this.schemaManager = schemaManager;
    this.dataFetcherExceptionHandler = dataFetcherExceptionHandler;
    this.authZManager = authZManager;
    this.introspectionManager = introspectionManager;
    this.executionMetricsManager = executionMetricsManager;
  }

  @Override
  public CompletableFuture<ExecutionResult> execute(final ExecutionInput executionInput) {

    GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator()
        .runtimeGraph(schemaManager.getRuntimeGraph())
        .queryExecutionStrategy(new AsyncExecutionStrategy(this.dataFetcherExceptionHandler));

    int instrumentationIdx = 0;

    if (introspectionManager.isIntrospectionNotEnabled()) {
      builder.instrumentation(instrumentationIdx, introspectionManager.getInstrumentation());
      ++instrumentationIdx;
    }

    if (authZManager.isAuthZEnabled()) {
      builder.instrumentation(instrumentationIdx, authZManager.getInstrumentation());
      ++instrumentationIdx;
    }

    if (executionMetricsManager.isExecutionMetricsEnabled()) {
      builder.instrumentation(instrumentationIdx, executionMetricsManager.getInstrumentation());
      // increment instrumentationIdx here if adding more or move to a different class
    }

    return builder.build().execute(executionInput);
  }

}
