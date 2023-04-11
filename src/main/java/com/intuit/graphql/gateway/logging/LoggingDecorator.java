package com.intuit.graphql.gateway.logging;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Flux;

public class LoggingDecorator extends ServerWebExchangeDecorator {

  private final ServerHttpRequestDecorator requestDecorator;
  private final ServerHttpResponseDecorator responseDecorator;
  private final TransactionContext tx;
  private final Logger log;
  private final Marker logData;

  LoggingDecorator(ServerWebExchange delegate, TransactionContext tx, Logger log, Marker logData) {
    super(delegate);
    this.tx = tx;
    this.log = log;
    this.logData = logData;
    requestDecorator = new LoggingRequestDecorator(delegate.getRequest());
    responseDecorator = new LoggingResponseDecorator(delegate.getResponse());
  }

  @Override
  public ServerHttpRequest getRequest() {
    return requestDecorator;
  }

  @Override
  public ServerHttpResponse getResponse() {
    return responseDecorator;
  }

  class LoggingRequestDecorator extends ServerHttpRequestDecorator {

    private final StringWriter cachedBody = new StringWriter();

    LoggingRequestDecorator(ServerHttpRequest delegate) {
      super(delegate);
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return super.getBody()
          .doOnNext(this::cache)
          .doOnComplete(() -> this.logRequest(cachedBody.toString()));
    }

    private void cache(DataBuffer buffer) {
      cachedBody.write(UTF_8.decode(buffer.asByteBuffer()).toString());
    }

    void logRequest(String requestBody) {
      Marker payloadData = MarkerUtil
          .addPayloadData(ContextFactory.getJson(requestBody), requestBody);
      if (payloadData != null) {
        logData.add(payloadData);
      }
      log.debug(logData, "Start Log Event");
    }
  }

  class LoggingResponseDecorator extends ServerHttpResponseDecorator {

    LoggingResponseDecorator(
        ServerHttpResponse delegate) {
      super(delegate);
    }
  }
}
