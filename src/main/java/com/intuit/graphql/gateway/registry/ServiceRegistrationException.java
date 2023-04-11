package com.intuit.graphql.gateway.registry;

/**
 * Error occurs during schema registration.
 */
public class ServiceRegistrationException extends RuntimeException {

  public ServiceRegistrationException(String msg) {
    super(msg);
  }

  public ServiceRegistrationException(String msg, Throwable t) {
    super(msg, t);
  }
}
