package com.intuit.graphql.gateway.introspection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import graphql.ErrorType;
import org.junit.Test;

public class IntrospectionAbortedGraphQLErrorTest {

  @Test
  public void canCreateInstance() {
    String errorMessage = "error message";
    IntrospectionAbortedGraphQLError actual = IntrospectionAbortedGraphQLError.builder()
        .message(errorMessage)
        .build();

    assertThat(actual.getMessage()).isEqualTo(errorMessage);
    assertThat(actual.getErrorType()).isEqualTo(ErrorType.ExecutionAborted);
  }

}
