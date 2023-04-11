package com.intuit.graphql.gateway.metrics;

import java.time.Instant;
import lombok.Getter;

@Getter
public class EventStopWatch {

  private Instant startTime;
  private Instant endTime;

  public EventStopWatch() {
    this.start();
  }

  public void start() {
    this.startTime = Instant.now();
    this.endTime = Instant.now();
  }

  public void stop() {
    this.endTime = Instant.now();
  }

}
