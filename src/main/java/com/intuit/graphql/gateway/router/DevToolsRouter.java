package com.intuit.graphql.gateway.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@Profile("!prod")
public class DevToolsRouter {

  public static final RequestPredicate GRAPHQL_PROVIDER_ROUTE = GET("/dev/graphql/providers");


  @Bean
  public RouterFunction<ServerResponse> devTools(ServiceRegistry serviceRegistry) {
    return route(GRAPHQL_PROVIDER_ROUTE,
        serverRequest -> ServerResponse.ok().body(serviceRegistry.getCachedRegistrations(), ServiceRegistration.class));
  }
}
