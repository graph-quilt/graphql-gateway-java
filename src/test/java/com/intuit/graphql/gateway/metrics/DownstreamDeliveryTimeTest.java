package com.intuit.graphql.gateway.metrics;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class DownstreamDeliveryTimeTest {

  @Test
  public void compute_emptyList_returnsZero() {
    long actual = DownstreamDeliveryTime.compute(Collections.emptyList());
    assertThat(actual).isEqualTo(0);
  }

  @Test
  public void compute_singleEvent_returnsElapseTimeOfEvent() {
    // given
    DownstreamCallEvent downstreamCallEvent = new DownstreamCallEvent("test-namespace","test-appId");
    EventStopWatch eventStopWatch = downstreamCallEvent.getEventStopWatch();
    eventStopWatch.stop();

    long expected = eventStopWatch.getEndTime().toEpochMilli() - eventStopWatch.getStartTime().toEpochMilli();

    List<DownstreamCallEvent> downstreamCallEvents = new ArrayList<>();
    downstreamCallEvents.add(downstreamCallEvent);


    // when
    long actual = DownstreamDeliveryTime.compute(downstreamCallEvents);

    // then
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void compute_consecutiveEvents_returnsElapseTimeOfEvent() throws InterruptedException {
    // given
    DownstreamCallEvent downstreamCallEvent1 = new DownstreamCallEvent("test-namespace1","test-appId4");
    DownstreamCallEvent downstreamCallEvent2 = new DownstreamCallEvent("test-namespace2","test-appId4");
    DownstreamCallEvent downstreamCallEvent3 = new DownstreamCallEvent("test-namespace3","test-appId4");

    EventStopWatch eventStopWatch1 = downstreamCallEvent1.getEventStopWatch();
    Thread.sleep(1);
    eventStopWatch1.stop();
    EventStopWatch eventStopWatch2 = downstreamCallEvent2.getEventStopWatch();
    Thread.sleep(2);
    eventStopWatch2.stop();
    EventStopWatch eventStopWatch3 = downstreamCallEvent3.getEventStopWatch();
    Thread.sleep(1);
    eventStopWatch3.stop();

    long expected = eventStopWatch3.getEndTime().toEpochMilli() - eventStopWatch1.getStartTime()
        .toEpochMilli();

    List<DownstreamCallEvent> downstreamCallEvents = new ArrayList<>();
    downstreamCallEvents.add(downstreamCallEvent1);
    downstreamCallEvents.add(downstreamCallEvent2);
    downstreamCallEvents.add(downstreamCallEvent3);

    // when
    long actual = DownstreamDeliveryTime.compute(downstreamCallEvents);

    // then
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void compute_interleaveEvents_returnsElapseTimeOfEvent() throws InterruptedException {
    DownstreamCallEvent downstreamCallEvent1 = new DownstreamCallEvent("test-namespace1","test-appId4");
    DownstreamCallEvent downstreamCallEvent2 = new DownstreamCallEvent("test-namespace2","test-appId4");
    DownstreamCallEvent downstreamCallEvent3 = new DownstreamCallEvent("test-namespace3","test-appId4");

    // given
    EventStopWatch eventStopWatch1 = downstreamCallEvent1.getEventStopWatch();
    Thread.sleep(1);
    EventStopWatch eventStopWatch2 = downstreamCallEvent2.getEventStopWatch();
    Thread.sleep(2);
    eventStopWatch2.stop();
    EventStopWatch eventStopWatch3 = downstreamCallEvent3.getEventStopWatch();
    Thread.sleep(1);
    eventStopWatch3.stop();
    Thread.sleep(1);
    eventStopWatch1.stop();

    List<DownstreamCallEvent> downstreamCallEvents = new ArrayList<>();
    downstreamCallEvents.add(downstreamCallEvent1);
    downstreamCallEvents.add(downstreamCallEvent2);
    downstreamCallEvents.add(downstreamCallEvent3);

    long expected = eventStopWatch1.getEndTime().toEpochMilli() - eventStopWatch1.getStartTime()
        .toEpochMilli();

    // when
    long actual = DownstreamDeliveryTime.compute(downstreamCallEvents);

    // then
    assertThat(actual).isEqualTo(expected);
  }

}
