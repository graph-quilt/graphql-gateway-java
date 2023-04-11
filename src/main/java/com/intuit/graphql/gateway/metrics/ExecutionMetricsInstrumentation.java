package com.intuit.graphql.gateway.metrics;

import static com.intuit.graphql.gateway.metrics.ExecutionMetrics.METRICS_DOWNSTREAM_DELIVERY_TIME;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.metrics.ExecutionMetrics.ExecutionMetricsData;
import com.intuit.graphql.gateway.webclient.TxProvider;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutionMetricsInstrumentation extends SimpleInstrumentation {

  @Override
  public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
    return new ExecutionMetricsInstrumentationState();
  }

  @Override
  public ExecutionContext instrumentExecutionContext(ExecutionContext executionContext,
      InstrumentationExecutionParameters parameters) {
    ExecutionMetricsInstrumentationState instrumentationState = parameters.getInstrumentationState();
    GraphQLContext graphQLContext = parameters.getContext();
    graphQLContext.put(ExecutionMetricsData.class, instrumentationState.getExecutionMetricsData());
    return executionContext;
  }

  @Override
  public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult,
      InstrumentationExecutionParameters parameters) {
    TxProvider.getTx(parameters.getContext()).ifPresent(tx -> {
      ExecutionMetricsInstrumentationState instrumentationState = parameters.getInstrumentationState();
      ExecutionMetrics executionMetrics = ExecutionMetrics.create(instrumentationState.getExecutionMetricsData());
      logMetrics(tx, executionMetrics);
    });

    return CompletableFuture.completedFuture(executionResult);
  }

  private void logMetrics(TransactionContext tx, ExecutionMetrics executionMetrics) {
    long downstreamTime = executionMetrics.getDownstreamDeliveryTime();
    if (Objects.nonNull(downstreamTime)) {
      LogNameValuePair namePairValueArray = ImmutableLogNameValuePair
          .of(METRICS_DOWNSTREAM_DELIVERY_TIME, downstreamTime);
      EventLogger.info(log, tx, "Execution Metrics", namePairValueArray);
    }
  }

  @AllArgsConstructor
  @Data
  static class ExecutionMetricsInstrumentationState implements InstrumentationState {
    private final ExecutionMetricsData executionMetricsData = new ExecutionMetricsData();
  }

}
