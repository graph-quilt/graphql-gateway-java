package com.intuit.graphql.gateway.metrics;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

import com.intuit.graphql.gateway.metrics.ExecutionMetrics.ExecutionMetricsData;
import org.junit.Test;

public class ExecutionMetricsDataTest {

  private final ExecutionMetricsData subject = new ExecutionMetricsData();

  @Test
  public void addServiceCallEventStopWatch_success() {
    DownstreamCallEvent downstreamCallEvent = mock(DownstreamCallEvent.class);
    subject.addDownstreamCallEvent(downstreamCallEvent);

    assertThat(subject.getDownstreamCallEvents()).hasSize(1);
  }


}
