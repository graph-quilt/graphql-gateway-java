package com.intuit.graphql.gateway.metrics;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

public class DownstreamDeliveryTime {

  private DownstreamDeliveryTime() {}

  public static long compute(List<DownstreamCallEvent> downstreamCallEvents) {
    if (CollectionUtils.isEmpty(downstreamCallEvents)) {
      return 0;
    }

    long earliestStartTime = Long.MAX_VALUE;
    long latestEndTime = 0;

    for (DownstreamCallEvent downstreamCallEvent : downstreamCallEvents) {
      EventStopWatch eventStopWatch = downstreamCallEvent.getEventStopWatch();
      earliestStartTime = Math.min(earliestStartTime, eventStopWatch.getStartTime().toEpochMilli());
      latestEndTime = Math.max(latestEndTime, eventStopWatch.getEndTime().toEpochMilli());
    }

    return latestEndTime - earliestStartTime;
  }

}
