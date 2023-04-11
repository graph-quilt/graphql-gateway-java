package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.logging.ContextFactory;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.SubtaskContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.context.Context;

@Slf4j
@Component
public class LoggingBatchLoaderHooks implements
    BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private static final String EXECUTION_TIMING_KEY = LoggingBatchLoaderHooks.class.getName() + ".key";

  @Override
  public void onBatchLoadStart(final GraphQLContext context, final List<DataFetchingEnvironment> batchLoaderKeys) {
    SubtaskContext subtaskContext = ContextFactory.getSubtaskContext("BatchLoader execution input generation");

    context.put(EXECUTION_TIMING_KEY, subtaskContext);
  }

  @Override
  public void onExecutionInput(final GraphQLContext context, final ExecutionInput executionInput) {
    TransactionContext tx = getTxFromContext(context);
    SubtaskContext subtaskContext = context.get(EXECUTION_TIMING_KEY);

    EventLogger.subtaskEnd(log, tx, subtaskContext);
  }

  @Override
  public void onQueryResult(final GraphQLContext context, final Map<String, Object> queryResult) {
    SubtaskContext subtaskContext = ContextFactory.getSubtaskContext("BatchLoader result transformation");

    context.put(EXECUTION_TIMING_KEY, subtaskContext);
  }

  @Override
  public void onBatchLoadEnd(final GraphQLContext context, final List<DataFetcherResult<Object>> batchLoaderResults) {
    TransactionContext tx = getTxFromContext(context);
    SubtaskContext subtaskContext = context.get(EXECUTION_TIMING_KEY);

    EventLogger.subtaskEnd(log, tx, subtaskContext);
  }

  private TransactionContext getTxFromContext(GraphQLContext context) {
    return context.<Context>getOrEmpty(Context.class)
        .flatMap(reactorContext -> reactorContext.<TransactionContext>getOrEmpty(TransactionContext.class))
        .orElse(null);
  }
}
