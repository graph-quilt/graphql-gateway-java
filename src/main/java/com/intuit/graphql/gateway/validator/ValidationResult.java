package com.intuit.graphql.gateway.validator;

import java.util.Objects;
import java.util.function.Consumer;

public class ValidationResult {

  protected Throwable throwable;

  public void doWhenInvalid(Consumer<Throwable> errConsumer) {
    if (Objects.nonNull(throwable)) {
      errConsumer.accept(throwable); // test is covered but not reported by jacoco
    }
  }
}
