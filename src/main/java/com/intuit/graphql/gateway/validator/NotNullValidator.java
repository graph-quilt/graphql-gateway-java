package com.intuit.graphql.gateway.validator;

import java.util.Objects;

public class NotNullValidator {

  private NotNullValidator() {
  }

  public static ValidationResult validate(Object... objects) {
    try {
      for (Object o : objects) {
        Objects.requireNonNull(o);
      }
      return new ValidResult();
    } catch (NullPointerException e) {
      return new NotValidResult(e);
    }
  }
}
