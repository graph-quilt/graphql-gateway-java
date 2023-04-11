package com.intuit.graphql.gateway.authorization;

import com.intuit.graphql.authorization.util.PrincipleFetcher;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.GraphQLContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

@Slf4j
public class AuthorizationFetcher implements PrincipleFetcher {

  public static final String AUTHZ_SCOPE = "authzscope";
  private final Set<String> clients;

  public AuthorizationFetcher(Set<String> clients) {
    this.clients = clients;
  }

  @Override
  public Set<String> getScopes(final Object o) {

    String authzScopeValue = getSingleHeader(o, AUTHZ_SCOPE);
    Set<String> authzScopes = Arrays.stream(authzScopeValue.split(","))
        .filter(scope -> this.clients.contains(scope))
        .collect(Collectors.toSet());

    TransactionContext tx = getTx(o).orElse(null);

    if (!authzScopes.isEmpty()) {
      EventLogger.info(log, tx, "Authorizing request");
      return authzScopes;
    }
    return Collections.emptySet();
  }

  @Override
  public boolean authzEnforcementExemption(final Object o) {
    //todo need to work on enforcement exemptions
    return false;
  }

  private String getSingleHeader(Object context, String headerName) {
    GraphQLContext graphQLContext = (GraphQLContext) context;
    ServerRequest serverRequest = graphQLContext.get(ServerRequest.class);
    return serverRequest.headers().asHttpHeaders().getFirst(headerName);
  }

  private Optional<TransactionContext> getTx(Object o) {
    if (o instanceof GraphQLContext) {
      final GraphQLContext graphQLContext = (GraphQLContext) o;
      return graphQLContext.<Context>getOrEmpty(Context.class)
          .flatMap(context -> context.getOrEmpty(TransactionContext.class));
    }

    return Optional.empty();
  }
}
