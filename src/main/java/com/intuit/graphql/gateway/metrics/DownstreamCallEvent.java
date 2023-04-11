package com.intuit.graphql.gateway.metrics;

import lombok.Getter;

@Getter
public class DownstreamCallEvent extends ExecutionEvent {

  private final String namespace;
  private final String appId;

  public DownstreamCallEvent(String namespace, String appId) {
    super.type = Type.DOWNSTREAM_CALL;
    this.namespace = namespace;
    this.appId = appId;
  }

}
