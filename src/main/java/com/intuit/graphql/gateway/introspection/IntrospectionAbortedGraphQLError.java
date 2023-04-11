package com.intuit.graphql.gateway.introspection;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphqlErrorException;
import java.util.ArrayList;
import java.util.HashMap;

public class IntrospectionAbortedGraphQLError extends GraphqlErrorException {

  private IntrospectionAbortedGraphQLError(IntrospectionAbortedGraphQLError.Builder builder) {
    super(builder);
  }

  @Override
  public ErrorClassification getErrorType() {
    return ErrorType.ExecutionAborted;
  }

  public static IntrospectionAbortedGraphQLError.Builder builder() {
    return new IntrospectionAbortedGraphQLError.Builder();
  }

  public static class Builder extends GraphqlErrorException
      .BuilderBase<IntrospectionAbortedGraphQLError.Builder, IntrospectionAbortedGraphQLError> {

    {
      super.extensions = new HashMap<>();
      super.sourceLocations = new ArrayList<>();
    }

    public IntrospectionAbortedGraphQLError build() {
      return new IntrospectionAbortedGraphQLError(this);
    }

  }

}
