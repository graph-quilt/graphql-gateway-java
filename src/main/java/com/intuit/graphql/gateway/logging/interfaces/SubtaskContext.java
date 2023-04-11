package com.intuit.graphql.gateway.logging.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.UUID;
import javax.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@JsonInclude(Include.NON_NULL)
@Immutable
public interface SubtaskContext extends LoggingContext {
  @Nullable
  @Default
  default String getRequestId() {
    return UUID.randomUUID().toString();
  }

  @Nullable
  @Parameter
  String getAction();
}
