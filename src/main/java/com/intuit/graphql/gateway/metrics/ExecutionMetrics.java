package com.intuit.graphql.gateway.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

public class ExecutionMetrics {

  public static final String METRICS_DOWNSTREAM_DELIVERY_TIME = "downstreamDeliveryTime";

  @Getter
  private long downstreamDeliveryTime;

  private ExecutionMetrics() {}

  public static ExecutionMetrics create(ExecutionMetricsData executionMetricsData) {
    Objects.requireNonNull(executionMetricsData);
    ExecutionMetrics executionMetrics = new ExecutionMetrics();
    executionMetrics.computeMetrics(executionMetricsData);
    return executionMetrics;
  }

  private void computeMetrics(ExecutionMetricsData executionMetricsData) {
    List<DownstreamCallEvent> downstreamCallEvents = executionMetricsData.getDownstreamCallEvents();
    downstreamDeliveryTime = DownstreamDeliveryTime.compute(downstreamCallEvents);
  }

  /**
   * Provides GraphQL Execution Metrics Data.
   *
   * An instance of this class should be created for each execution context.
   */
  @Getter
  public static class ExecutionMetricsData {

    private final List<DownstreamCallEvent> downstreamCallEvents = new ArrayList<>();

    public void addDownstreamCallEvent(DownstreamCallEvent downstreamCallEvent) {
      Objects.requireNonNull(downstreamCallEvent);
      downstreamCallEvents.add(downstreamCallEvent);
    }

  }

}
