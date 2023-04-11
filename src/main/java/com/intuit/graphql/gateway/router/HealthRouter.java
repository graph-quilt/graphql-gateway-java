package com.intuit.graphql.gateway.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.intuit.graphql.gateway.handler.HealthRouteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class HealthRouter {

  private final HealthRouteHandler healthRouteHandler;

  static final RequestPredicate healthRoute = GET("/health/local");

  public HealthRouter(final HealthRouteHandler healthRouteHandler) {
    this.healthRouteHandler = healthRouteHandler;
  }

  @Bean
  public RouterFunction<ServerResponse> healthRouterFunction() {
    return route(healthRoute, healthRouteHandler::handle);
  }

}
