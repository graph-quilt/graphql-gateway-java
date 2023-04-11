package com.intuit.graphql.gateway.webfilter;

import com.intuit.graphql.gateway.logging.ContextFactory;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ExternalContext;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * An Exchange filter that logs all requests delivered through a WebClient.
 *
 * <p>There will be two log entries:
 * <ul>
 *   <li>Sub-task Start with message {@code Start request} at the beginning of the request</li>
 *   <li>Sub-task End with message {@code End request} at the end of the request upon receiving a response or if an error occurred</li>
 * </ul>
 *
 * <p>The HTTP Status of the response will be printed in the sub-task end as {@code httpStatus}, if it exists.
 *
 * <p>The filter also allows for arbitrary hints to be set in the {@code SubscriberContext}. These hints will be logged on both
 * log entries as JSON objects with as {@code hints}.
 *
 * <p><b>NOTE: Hints must be a Map&#060;String, Object&#062; or else your hints will not be logged!</b>
 *
 * <p>Example code:
 *
 * <pre class="code">
 * Map&#060;String, Object&#062; hints = new HashMap&#060;&#062;();
 *
 * hints.put("requestType", "MyRequestType");
 * hints.put("someObject", myObject);
 *
 * webClient.get().uri("http://google.com")
 *   .exchange()
 *   .subscriberContext(context -&#062; context.put(RequestLoggingExchangeFilter.REQUEST_LOGGING_HINTS, hints));
 * </pre>
 * <p>
 * Below is an example log output for both entries.
 *
 * <pre class="code">
 * {
 *   "message": "Start request",
 *   "subtaskContext": {
 *      "initiatedBy": "External Request"
 *      "method": "GET"
 *      "requestId": "abcd1234-cdcf-419b-9140-acea3748b34e"
 *      "url": "http://google.com"
 *   }
 *   ...
 *   "hints": {
 *     "requestType": "MyRequestType",
 *     "someObject": {
 *       "key": "value"
 *     }
 *   }
 * }
 *
 * {
 *   "message": "End request",
 *   "httpStatus": 200,
 *   "subtaskContext": {
 *      "initiatedBy": "External Request"
 *      "method": "GET"
 *      "requestId": "abcd1234-cdcf-419b-9140-acea3748b34e"
 *      "url": "http://google.com",
 *      "durationMs": 10
 *      "durationNano": 10000000
 *   }
 *   ...
 *   "hints": {
 *     "requestType": "MyRequestType",
 *     "someObject": {
 *       "key": "value"
 *     }
 *   }
 * }
 *
 * </pre>
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class RequestLoggingExchangeFilter implements ExchangeFilterFunction {

  public static final String REQUEST_LOGGING_HINTS =
      RequestLoggingExchangeFilter.class.getName() + ".hints";
  static final String EXTERNAL_CONTEXT_KEY =
      RequestLoggingExchangeFilter.class.getName() + ".externalContext";
  static final String HINTS_KEY = "hints";
  static final String HTTP_STATUS_KEY = "httpStatus";
  static final String SUBTASK_START_MESSAGE = "Start request";
  static final String SUBTASK_END_MESSAGE = "End request";
  private static final LogNameValuePair GATEWAY_TIMEOUT = ImmutableLogNameValuePair.of(HTTP_STATUS_KEY, 504);
  private static final String EXTERNAL_REQUEST_MESSAGE = "External Request";

  @Override
  public Mono<ClientResponse> filter(@NonNull final ClientRequest request,
      @NonNull final ExchangeFunction next) {
    return getHintsFromAttributes(request)
        .map(hints -> logThenExchange(next, request, hints))
        .orElseGet(() -> logThenExchange(next, request, null));
  }

  private Mono<ClientResponse> logThenExchange(final ExchangeFunction next,
      final ClientRequest request, Map hints) {
    return Mono.subscriberContext()
        .flatMap(context -> {
          TransactionContext tx = context.<TransactionContext>getOrEmpty(TransactionContext.class)
              .orElse(null);
          ExternalContext ex = context.get(EXTERNAL_CONTEXT_KEY);
          List<LogNameValuePair> nvPairs = new ArrayList<>(2);

          if (hints != null) {
            nvPairs.add(ImmutableLogNameValuePair.of(HINTS_KEY, hints));
          }

          return next.exchange(request)
              //doOnSubscribe is a separate entry not in doOnEach due to a reactor-core issue
              //https://github.com/reactor/reactor-core/issues/1526
              .doOnSubscribe(subscription -> EventLogger
                  .subtaskStart(log, tx, ex, SUBTASK_START_MESSAGE,
                      nvPairs.toArray(new LogNameValuePair[2])))
              .doOnEach(signal -> {
                if (!signal.isOnComplete()) {
                  if (signal.isOnError()) {
                    Optional.of(signal.getThrowable())
                        .filter(throwable -> throwable instanceof HttpStatusCodeException)
                        .ifPresent(throwable -> nvPairs.add(ImmutableLogNameValuePair
                            .of(HTTP_STATUS_KEY,
                                ((HttpStatusCodeException) throwable).getRawStatusCode())));
                  } else {
                    Optional.ofNullable(signal.get())
                        .ifPresent(clientResponse -> nvPairs.add(
                            ImmutableLogNameValuePair
                                .of(HTTP_STATUS_KEY, clientResponse.statusCode().value())));
                  }
                }
              })
              .doFinally(signalType -> {
                if (SignalType.CANCEL.equals(signalType) && nvPairs.stream()
                    .noneMatch(nvPair -> nvPair.getName().equals(HTTP_STATUS_KEY))) {
                  nvPairs.add(GATEWAY_TIMEOUT);
                }
                EventLogger.subtaskEnd(log, tx, ex, SUBTASK_END_MESSAGE,
                    nvPairs.toArray(new LogNameValuePair[2]));
              });
        })
        .subscriberContext(
            context -> context.put(EXTERNAL_CONTEXT_KEY, buildExternalContext(request)));
  }


  private ExternalContext buildExternalContext(ClientRequest request) {
    return ContextFactory.getExternalContext(EXTERNAL_REQUEST_MESSAGE, request.method().toString(),
        request.url().toString());
  }

  private Optional<Map> getHintsFromAttributes(ClientRequest clientRequest) {
    return clientRequest.attribute(REQUEST_LOGGING_HINTS)
        .flatMap(map -> map instanceof Map ? Optional.of((Map) map) : Optional.empty());
  }
}