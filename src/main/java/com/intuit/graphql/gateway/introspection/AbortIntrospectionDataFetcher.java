package com.intuit.graphql.gateway.introspection;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class AbortIntrospectionDataFetcher implements DataFetcher<Object> {

  @Override
  public Object get(DataFetchingEnvironment environment) throws Exception {
    throw IntrospectionAbortedGraphQLError.builder()
        .message("Introspection aborted.")
        .build();
  }
}
