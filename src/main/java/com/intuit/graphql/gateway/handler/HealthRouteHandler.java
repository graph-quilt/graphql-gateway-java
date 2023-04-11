package com.intuit.graphql.gateway.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class HealthRouteHandler {

  public Mono<ServerResponse> handle(ServerRequest serverRequest) {
    return ServerResponse.ok().syncBody("Health Check OK");
  }
}
