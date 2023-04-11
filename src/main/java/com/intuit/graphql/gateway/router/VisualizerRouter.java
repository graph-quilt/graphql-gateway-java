package com.intuit.graphql.gateway.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.intuit.graphql.gateway.handler.VisualizerRouteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Component
public class VisualizerRouter {

  private static final String VISUALIZER_ENDPOINT = "/graphiql/visualizer";

  static final RequestPredicate visualizerRoute = GET(VISUALIZER_ENDPOINT);

  private final VisualizerRouteHandler visualizerRouteHandler;

  public VisualizerRouter(VisualizerRouteHandler visualizeRouteHandler) {
    this.visualizerRouteHandler = visualizeRouteHandler;
  }

  @Bean
  public RouterFunction<ServerResponse> visualizerRouterFunction() {
    return route(visualizerRoute, visualizerRouteHandler::handle);
  }

}
