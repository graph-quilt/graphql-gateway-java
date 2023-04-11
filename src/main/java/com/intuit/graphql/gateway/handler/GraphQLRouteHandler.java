package com.intuit.graphql.gateway.handler;

import static com.intuit.graphql.gateway.graphql.GraphQLRequest.SPECIFICATION_TYPE_REFERENCE;

import com.intuit.graphql.gateway.graphql.GraphQLExecutor;
import com.intuit.graphql.gateway.graphql.GraphQLRequest;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.ErrorType;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
import graphql.GraphQLError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
@Slf4j
public class GraphQLRouteHandler {

  private GraphQLExecutor<ExecutionInput> graphQLExecutor;

  public GraphQLRouteHandler(GraphQLExecutor<ExecutionInput> graphQLExecutor) {
    this.graphQLExecutor = graphQLExecutor;
  }

  public Mono<ServerResponse> handle(ServerRequest serverRequest) {
    return Mono.subscriberContext()
        .flatMap(subscriberContext -> {
          TransactionContext tx = subscriberContext.get(TransactionContext.class);
          return (serverRequest.bodyToMono(GraphQLRequest.class)
              .doOnError(err -> EventLogger.error(log, tx, "Failed to parse query", err)))
              .map(graphQLRequest -> this.getExecutionResult(subscriberContext, graphQLRequest, serverRequest))
              .flatMap(result -> ServerResponse.ok().body(result, SPECIFICATION_TYPE_REFERENCE));
        });
  }

  private Mono<Map<String, Object>> getExecutionResult(final Context subscriberContext,
      final GraphQLRequest graphQLRequest, final ServerRequest serverRequest) {
    TransactionContext tx = subscriberContext.get(TransactionContext.class);

    GraphQLContext graphQLContext = GraphQLContext.newContext()
        .of(Context.class, subscriberContext)
        .of(ServerRequest.class, serverRequest).build();

    final ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .query(graphQLRequest.query())
        .variables(graphQLRequest.variables() == null ? new HashMap<>() : graphQLRequest.variables())
        .operationName(graphQLRequest.operationName())
        .context(graphQLContext)
        .build();

    return Mono.fromFuture(() -> graphQLExecutor.execute(executionInput))
        .doOnError(err -> EventLogger.error(log, tx, "Failed to execute query", err))
        .map(executionResult -> {
          logQueryValidationErrorsIfAny(executionResult, tx);
          return executionResult;
        })
        .map(ExecutionResult::toSpecification);
  }

  private void logQueryValidationErrorsIfAny(ExecutionResult executionResult, TransactionContext tx) {
    List<GraphQLError> graphQLErrors = executionResult.getErrors();
    if (CollectionUtils.isNotEmpty(graphQLErrors)) {
      graphQLErrors.stream().forEach(graphQLError -> {
        if (graphQLError.getErrorType() == ErrorType.ValidationError) {
          EventLogger.error(log, tx, graphQLError.getMessage());
        }
      });
    }
  }
}
