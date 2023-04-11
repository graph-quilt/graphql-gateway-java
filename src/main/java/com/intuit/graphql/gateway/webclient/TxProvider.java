package com.intuit.graphql.gateway.webclient;

import com.intuit.graphql.gateway.config.properties.AppLoggingProperties;
import com.intuit.graphql.gateway.config.properties.AppSecurityProperties;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.GraphQLContext;
import graphql.execution.ExecutionContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
public class TxProvider {

  private final AppSecurityProperties appSecurityProperties;
  private final AppLoggingProperties loggingProperties;

  public TxProvider(AppSecurityProperties appSecurityProperties,
      AppLoggingProperties loggingProperties) {
    this.appSecurityProperties = appSecurityProperties;
    this.loggingProperties = loggingProperties;
  }


  /**
   * Returns a default context with contextual service information set.
   *
   * <pre>
   *   appId (property driven)
   *   env (property driven)
   *   version (property driven)
   *   offeringId (property driven)
   *   security scan (false)
   *   test (false)
   *   tid (random)
   * </pre>
   *
   * @return a new TransactionContext with contextual service information
   */
  public TransactionContext newTx() {
    return ImmutableTransactionContext
        .builder()
        .appId(loggingProperties.getAppId())
        .env(loggingProperties.getEnv())
        .version(loggingProperties.getVersion())
        .tid(UUID.randomUUID().toString())
        .build();
  }

  /**
   * Returns a default context with contextual service information set. Provides ability to use custom tid
   *
   * <pre>
   *   appId (property driven)
   *   env (property driven)
   *   version (property driven)
   *   offeringId (property driven)
   *   security scan (false)
   *   test (false)
   * </pre>
   *
   * @param tid Transaction id
   * @return a new TransactionContext with contextual service information
   */
  public TransactionContext newTx(final String tid) {
    return ImmutableTransactionContext
        .builder()
        .appId(loggingProperties.getAppId())
        .env(loggingProperties.getEnv())
        .version(loggingProperties.getVersion())
        .tid(tid)
        .build();
  }

  public static TransactionContext emptyTx() {
    return ImmutableTransactionContext
        .builder()
        .tid("empty-" + UUID.randomUUID().toString())
        .build();
  }

  /**
   * Returns the transaction context embedded inside the Mono Sequence.
   *
   * @return TransactionContext for the subscriber context or a default requestTx if one does not exist.
   */
  public static Mono<TransactionContext> embeddedTx() {
    return Mono.subscriberContext()
        .map(context -> context.getOrDefault(TransactionContext.class, emptyTx()));
  }

  /**
   * returns the TransactionContext if present in a contextObject.
   *
   * contextObject is usually acquired from {@link ExecutionContext#getContext()} which returns an {@link Object} type.
   *
   * @param contextObject context object acquired from graphql context. Example {@link ExecutionContext#getContext()}
   * @return an optional TransactionContext
   */
  public static Optional<TransactionContext> getTx(Object contextObject) {
    if (contextObject instanceof GraphQLContext) {
      final GraphQLContext graphQLContext = (GraphQLContext) contextObject;
      return graphQLContext.<Context>getOrEmpty(Context.class)
          .flatMap(context -> context.getOrEmpty(TransactionContext.class));
    }

    return Optional.empty();
  }
}
