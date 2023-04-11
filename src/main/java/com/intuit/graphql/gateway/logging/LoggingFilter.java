package com.intuit.graphql.gateway.logging;

import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static net.logstash.logback.marker.Markers.append;

import com.intuit.graphql.gateway.config.properties.AppLoggingProperties;
import com.intuit.graphql.gateway.logging.interfaces.ContextFactoryInput;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableContextFactoryInput;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.Marker;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
@Slf4j
public class LoggingFilter implements WebFilter, Ordered {

  private final AppLoggingProperties props;

  private static final Set<String> PROD_LIKE_ENVS = new HashSet<>(
      Arrays.asList("PROD", "PRD", "PERF", "PRF"));

  private static final String END_LOG_PROPERTIES = "endLogProperties";

  private static final String PARAMETERS_FIELD_NAME = "parameters";
  private static final String HEADERS_FIELD_NAME = "headers";
  private static final String COOKIES_FIELD_NAME = "cookies";

  private boolean shouldLogQueryParameters(MultiValueMap<String, String> queryParameters) {
    return queryParameters != null && !queryParameters.isEmpty();
  }

  private Marker getLogStartMarker(TransactionContext tx, ServerHttpRequest request) {
    Marker logData = MarkerUtil.generateMarker("start", tx, null);

    MultiValueMap<String, String> params = request.getQueryParams();
    if (shouldLogQueryParameters(params)) {
      logData.add(Markers.append(PARAMETERS_FIELD_NAME, params));
    }
    Map<String, String> headers = getHeaders(request.getHeaders());
    if (!headers.isEmpty()) {
      logData.add(Markers.append(HEADERS_FIELD_NAME, headers));
    }
    Map<String, String> cookies = getCookies(request.getCookies());
    if (!cookies.isEmpty()) {
      logData.add(Markers.append(COOKIES_FIELD_NAME, cookies));
    }

    return logData;
  }

  private LogNameValuePair[] getLogStartNameValuePairs(ServerHttpRequest request) {
    List<LogNameValuePair> pairs = new ArrayList<>();

    MultiValueMap<String, String> params = request.getQueryParams();
    if (shouldLogQueryParameters(params)) {
      pairs.add(
          ImmutableLogNameValuePair.of(PARAMETERS_FIELD_NAME, params)
      );
    }
    Map<String, String> headers = getHeaders(request.getHeaders());
    if (!headers.isEmpty()) {
      pairs.add(
          ImmutableLogNameValuePair.of(HEADERS_FIELD_NAME, headers)
      );
    }
    Map<String, String> cookies = getCookies(request.getCookies());
    if (!cookies.isEmpty()) {
      pairs.add(
          ImmutableLogNameValuePair.of(COOKIES_FIELD_NAME, cookies)
      );
    }

    return pairs.toArray(new LogNameValuePair[0]);
  }

  public LoggingFilter(AppLoggingProperties props) {
    this.props = props;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
    try {
      ServerHttpRequest request = serverWebExchange.getRequest();

      Function<String, String> getHeader = (String header) -> String
          .join(",", request.getHeaders().getOrDefault(header, Collections.emptyList()));

      ContextFactoryInput input = ImmutableContextFactoryInput.builder()
          .method(request.getMethod().name()).requestUri(request.getURI().getPath())
          .app(props.getAppId())
          .contentType(request.getHeaders().getFirst("Content-Type"))
          .env(props.getEnv()).version(props.getVersion()).headerValue(getHeader).build();

      TransactionContext tx = ContextFactory.getTransactionContext(input);

      ServerWebExchange exchange;

      if (log.isDebugEnabled() && request.getMethod()
          .equals(HttpMethod.POST)) {
        Marker logData = getLogStartMarker(tx, request);
        exchange = new LoggingDecorator(serverWebExchange, tx, log, logData);
      } else {
        LogNameValuePair[] requestMetadata = getLogStartNameValuePairs(request);
        EventLogger.startWithoutHttpServletRequest(log, tx, "Start Log Event", requestMetadata);
        exchange = serverWebExchange;
      }

      ServerHttpResponse response = exchange.getResponse();
      response.beforeCommit(() -> this.logResponse(response, tx));

      return webFilterChain.filter(exchange)
          .subscriberContext((Context context) -> context.put(TransactionContext.class, tx))
          .subscriberContext(c -> c.put(END_LOG_PROPERTIES, new HashMap<String, Object>()));
    } catch (Exception e) {
      log.error("Uncaught exception in LoggingFilter: ", e);
      return Mono.error(e);
    }
  }

  Mono<Void> logResponse(ServerHttpResponse response, TransactionContext tx) {
    Marker logData = MarkerUtil.generateMarker("end", tx, null);
    return Mono
        .subscriberContext()
        .map(this::getEndLogProperties)
        .flatMap(additionalProperties -> {
          logData.add(Markers.append("headers", getHeaders(response.getHeaders())));
          logData.add(Markers.append("httpStatus", response.getStatusCode().value()));
          logData.add(append("durationMs", System.currentTimeMillis() - tx.getMsStart()));
          logData.add(append("durationNano", System.nanoTime() - tx.getNanoStart()));
          if (!additionalProperties.isEmpty()) {
            logData.add(append("additionalProperties", additionalProperties));
          }
          log.info(logData, "End Log Event");
          return Mono.empty();
        });
  }

  static Map<String, String> getHeaders(HttpHeaders headers) {
    Map<String, String> headersMap = new TreeMap<>();
    headers.entrySet().forEach(header -> header.getValue().forEach(headerVal -> {
          if (!header.getKey().equalsIgnoreCase(COOKIE)) {
            headersMap.put(header.getKey().toLowerCase(), headerVal);
          }
        }
    ));
    return headersMap;
  }

  static Map<String, String> getCookies(MultiValueMap<String, HttpCookie> cookies) {
    Map<String, String> cookiesMap = new HashMap<>();
    cookies.entrySet().forEach(cookie -> cookie.getValue().forEach(cookieVal -> cookiesMap
        .put(cookie.getKey(), cookieVal.getValue())));
    return cookiesMap;
  }

  private Map<String, Object> getEndLogProperties(Context context) {
    try {
      return (Map<String, Object>) context.get(END_LOG_PROPERTIES);
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  @Override
  public int getOrder() {
    return props.getOrder();
  }
}

