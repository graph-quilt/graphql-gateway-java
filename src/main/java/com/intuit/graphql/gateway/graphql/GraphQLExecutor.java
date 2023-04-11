package com.intuit.graphql.gateway.graphql;

import graphql.ExecutionResult;
import java.util.concurrent.CompletableFuture;

public interface GraphQLExecutor<T> {

  /**
   * Executes a GraphQL query.
   *
   * @param executionInput The input to the GraphQL query
   * @return The result of the GraphQL query
   */
  CompletableFuture<ExecutionResult> execute(T executionInput);

}
