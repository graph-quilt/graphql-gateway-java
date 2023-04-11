package com.intuit.graphql.gateway.logging.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Used to track any activity a part of the transaction - typically it will be an external call to another service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Value.Immutable
public interface ExternalContext extends LoggingContext {

  String getMethod();

  String getUrl();

  @Value.Default
  @Nullable
  default String getRequestId() {
    return UUID.randomUUID().toString();
  }

  @Nullable
  String getInitiatedBy();

}
