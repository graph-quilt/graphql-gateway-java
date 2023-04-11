package com.intuit.graphql.gateway.logging.interfaces;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable(
    builder = false
)
public interface LogNameValuePair {

  @Parameter
  String getName();

  @Nullable
  @Parameter
  Object getValue();
}

