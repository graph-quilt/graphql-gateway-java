package com.intuit.graphql.gateway.logging.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

@JsonInclude(Include.NON_EMPTY)
@Immutable
public interface TransactionContext extends Timed {

  @Nullable
  String getApp();

  @Nullable
  String getEnv();

  @Nullable
  String getVersion();

  @Nullable
  String getTid();

  @Nullable
  String getMethod();

  @Nullable
  String getPath();

  @Nullable
  String getContentType();

  @Nullable
  String getAppId();

  @Nullable
  String getIpAddress();
}

