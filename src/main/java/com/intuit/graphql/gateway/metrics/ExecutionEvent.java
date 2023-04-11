package com.intuit.graphql.gateway.metrics;

import lombok.Getter;

@Getter
public abstract class ExecutionEvent {
  protected Type type;
  protected EventStopWatch eventStopWatch = new EventStopWatch();

  public enum Type {
    DOWNSTREAM_CALL
  }
}
