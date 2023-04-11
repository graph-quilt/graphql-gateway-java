package com.intuit.graphql.gateway.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.intuit.graphql.gateway.handler.GraphiQLRouteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class GraphiQLRouter {

  private static final String GRAPHIQL_ENDPOINT = "/graphiql";

  static final RequestPredicate graphiQLRoute = GET(GRAPHIQL_ENDPOINT);

  private final GraphiQLRouteHandler graphiQLRouteHandler;

  public GraphiQLRouter(GraphiQLRouteHandler graphiQLRouteHandler) {
    this.graphiQLRouteHandler = graphiQLRouteHandler;
  }

  @Bean
  public RouterFunction<ServerResponse> graphiQlRouterFunction() {
    return route(graphiQLRoute, graphiQLRouteHandler::handle);
  }

}
