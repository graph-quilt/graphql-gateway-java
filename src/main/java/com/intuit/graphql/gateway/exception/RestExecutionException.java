package com.intuit.graphql.gateway.exception;

import com.intuit.graphql.gateway.registry.ServiceDefinition;
import lombok.Getter;

@Getter
public class RestExecutionException extends RuntimeException {

  private final ServiceDefinition definition;

  public RestExecutionException(String message, Throwable cause, ServiceDefinition definition) {
    super(message, cause);
    this.definition = definition;
  }
}
