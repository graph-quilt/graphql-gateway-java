package com.intuit.graphql.gateway.handler;

import static org.springframework.http.MediaType.TEXT_HTML;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GraphiQLRouteHandler {

  private final ResourceLoader resourceLoader;
  private final String staticContent;

  private static final String INDEX_LOCATION = String.format("classpath:graphiql.html");
  public static final String SUCCESS_MSG = "Serving graphiql.html page";
  public static final String ERROR_MSG = "Could not find 'graphiql.html' for serving Graphiql";


  public GraphiQLRouteHandler(ResourceLoader resourceLoader) throws IOException {
    this.resourceLoader = resourceLoader;
    this.staticContent = IOUtils
        .toString(this.resourceLoader.getResource(INDEX_LOCATION).getInputStream(), StandardCharsets.UTF_8);
  }

  public Mono<ServerResponse> handle(ServerRequest serverRequest) {

    return Mono.subscriberContext()
        .map(context -> context.get(TransactionContext.class))
        .flatMap(tx -> ServerResponse.ok().contentType(TEXT_HTML).syncBody(staticContent)
            .doOnSuccess(response -> EventLogger.info(log, tx, SUCCESS_MSG))
            .doOnError(err -> EventLogger.error(log, tx, ERROR_MSG, err))
        );
  }
}
