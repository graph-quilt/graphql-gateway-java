package com.intuit.graphql.gateway.validator;

public class NotValidResult extends ValidationResult {

  public NotValidResult(Throwable t) {
    this.throwable = t;
  }
}
