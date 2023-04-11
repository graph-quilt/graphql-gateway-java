package com.intuit.graphql.gateway.provider;

import com.intuit.graphql.gateway.graphql.DataRetrieverException;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import graphql.GraphQLContext;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

public class ServiceProviderHelper {

  public static void validate(final ServiceDefinition serviceDefinition, final GraphQLContext context) {
    Context reactorCtx = context.get(Context.class);
    Objects.requireNonNull(reactorCtx, "requires Reactor Context");

    TransactionContext txnCtx = reactorCtx.get(TransactionContext.class);
    Objects.requireNonNull(txnCtx, "requires TransactionContext");

    Objects.requireNonNull(context.get(ServerRequest.class), "requires ServerRequest");

    if (!isClientWhitelisted(serviceDefinition, txnCtx)) {
      throw new DataRetrieverException(HttpStatus.FORBIDDEN.getReasonPhrase());
    }
  }

  private static boolean isClientWhitelisted(final ServiceDefinition serviceDefinition,
      final TransactionContext txnCtx) {

    return serviceDefinition.getClientWhitelist().isEmpty() || Optional.ofNullable(txnCtx.getAppId())
        .map(appId -> serviceDefinition.getClientWhitelist().contains(appId))
        .orElse(Boolean.FALSE);
  }
}
