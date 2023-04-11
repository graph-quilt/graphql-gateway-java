package com.intuit.graphql.gateway.validator;

import java.util.Objects;

public class EqualsValidator {

  private EqualsValidator() {
  }

  public static ValidationResult validate(Object expected, Object actual) {
    if (Objects.equals(expected, actual)) {
      return new ValidResult();
    } else {
      return new NotValidResult(new AssertionError(String
          .format("Comparison Failure:%nExpected: %s %nActual  : %s", Objects.toString(expected),
              Objects.toString(actual))));
    }
  }
}


