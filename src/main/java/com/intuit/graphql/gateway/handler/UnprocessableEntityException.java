package com.intuit.graphql.gateway.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnprocessableEntityException extends ResponseStatusException {
  public UnprocessableEntityException(String reason) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, reason);
  }
}