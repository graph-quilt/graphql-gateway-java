package com.intuit.graphql.gateway.logging.interfaces;

import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import org.immutables.value.Value.Immutable;

@Immutable
public interface ContextFactoryInput {
  @Nullable
  String contentType();

  String method();

  String requestUri();

  @Nullable
  String payload();

  String app();

  String version();

  String env();

  Function<String, String> headerValue();

  @Nullable
  Supplier<Cookie[]> cookies();
}
