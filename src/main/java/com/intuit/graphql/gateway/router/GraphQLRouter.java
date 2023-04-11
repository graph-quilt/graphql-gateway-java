package com.intuit.graphql.gateway.router;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.intuit.graphql.gateway.handler.GraphQLRouteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class GraphQLRouter {

  private static final String GRAPHQL_ENDPOINT = "/graphql";

  static final RequestPredicate graphQLRoute = POST(GRAPHQL_ENDPOINT)
      .and(accept(APPLICATION_JSON)
          .and(contentType(APPLICATION_JSON))
      );

  private final GraphQLRouteHandler graphQLRouteHandler;

  public GraphQLRouter(GraphQLRouteHandler graphQLRouteHandler) {
    this.graphQLRouteHandler = graphQLRouteHandler;
  }

  @Bean
  public RouterFunction<ServerResponse> graphQlRouterFunction() {
    return route(graphQLRoute, graphQLRouteHandler::handle);
  }
}
