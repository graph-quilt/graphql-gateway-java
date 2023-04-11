package com.intuit.graphql.gateway.graphql;

public class DataRetrieverException extends RuntimeException {

  public DataRetrieverException(String msg) {
    super(msg);
  }

  public DataRetrieverException(String msg, Throwable t) {
    super(msg, t);
  }

}
