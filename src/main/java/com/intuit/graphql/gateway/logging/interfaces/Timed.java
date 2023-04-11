package com.intuit.graphql.gateway.logging.interfaces;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value.Default;

public interface Timed {
  @JsonIgnore
  @Default
  default long getMsStart() {
    return System.currentTimeMillis();
  }

  @JsonIgnore
  @Default
  default long getNanoStart() {
    return System.nanoTime();
  }
}