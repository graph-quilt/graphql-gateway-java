package com.intuit.graphql.gateway;

import static com.intuit.graphql.gateway.webclient.TxProvider.emptyTx;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
@Slf4j
public class CustomErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

  public static final String TRANSACTION_CONTEXT = "transactionContext";

  public CustomErrorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer, ServerProperties serverProperties) {
    super(errorAttributes, webProperties.getResources(), serverProperties.getError(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    TransactionContext tx = (TransactionContext) request.attribute(TRANSACTION_CONTEXT).orElse(emptyTx());
    EventLogger.error(log, tx, "Global Exception Caught");
    return super.renderErrorResponse(request);

  }
}

