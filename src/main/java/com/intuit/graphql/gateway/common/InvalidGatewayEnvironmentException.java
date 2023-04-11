package com.intuit.graphql.gateway.common;

import com.intuit.graphql.gateway.s3.S3ServiceDefinition.GatewayEnvironment;

/**
 * This exception should be thrown if a given environment is not
 * one of the values defined in {@link GatewayEnvironment}
 */
public class InvalidGatewayEnvironmentException extends RuntimeException {

  public InvalidGatewayEnvironmentException(String message) {
    super(message);
  }
}
