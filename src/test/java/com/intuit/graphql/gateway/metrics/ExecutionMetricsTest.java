package com.intuit.graphql.gateway.metrics;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.metrics.ExecutionMetrics.ExecutionMetricsData;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionMetricsTest {

  @Mock
  private ExecutionMetricsData executionMetricsDataMock;

  private static final List<DownstreamCallEvent> downstreamCallEvents = new ArrayList<>();
  private static long expectedDownstreamDeliveryTime;

  @BeforeClass
  public static void setup() throws InterruptedException {
    DownstreamCallEvent downstreamCallEvent1 = new DownstreamCallEvent("test-namespace", "test-appId");
    DownstreamCallEvent downstreamCallEvent2 = new DownstreamCallEvent("test-namespace", "test-appId");
    DownstreamCallEvent downstreamCallEvent3 = new DownstreamCallEvent("test-namespace", "test-appId");
    EventStopWatch eventStopWatch1 = downstreamCallEvent1.getEventStopWatch();
    Thread.sleep(1);
    eventStopWatch1.stop();
    EventStopWatch eventStopWatch2 = downstreamCallEvent2.getEventStopWatch();
    Thread.sleep(1);
    eventStopWatch2.stop();
    EventStopWatch eventStopWatch3 = downstreamCallEvent2.getEventStopWatch();
    Thread.sleep(1);
    eventStopWatch3.stop();

    expectedDownstreamDeliveryTime = eventStopWatch3.getEndTime().toEpochMilli() -
        eventStopWatch1.getStartTime().toEpochMilli();

    downstreamCallEvents.add(downstreamCallEvent1);
    downstreamCallEvents.add(downstreamCallEvent2);
    downstreamCallEvents.add(downstreamCallEvent3);
  }

  @Test
  public void create_success() {
    // given
    when(executionMetricsDataMock.getDownstreamCallEvents()).thenReturn(downstreamCallEvents);

    // when
    ExecutionMetrics actual = ExecutionMetrics.create(executionMetricsDataMock);

    // then
    assertThat(actual.getDownstreamDeliveryTime()).isEqualTo(expectedDownstreamDeliveryTime);
  }


}
