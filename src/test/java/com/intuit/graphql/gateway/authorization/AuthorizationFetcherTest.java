package com.intuit.graphql.gateway.authorization;

import static com.intuit.graphql.gateway.authorization.AuthorizationFetcher.AUTHZ_SCOPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.GraphQLContext;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

public class AuthorizationFetcherTest {

  @Test
  public void testAllowsClientsWithoutRules() {
    final AuthorizationFetcher fetcher = new AuthorizationFetcher(Collections.emptySet());
    MockServerRequest serverRequest = MockServerRequest.builder()
        .header(AUTHZ_SCOPE, "test_assetalias")
        .build();

    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .build();

    assertThat(fetcher.getScopes(context)).isEmpty();
  }


  @Test
  public void testReturnsAssetAliasScopeForClientsWithRules() {
    final AuthorizationFetcher fetcher = new AuthorizationFetcher(Collections.singleton("test_assetalias"));

    MockServerRequest serverRequest = MockServerRequest.builder()
        .header(AUTHZ_SCOPE, "test_assetalias")
        .build();

    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .of(Context.class, Context.of(TransactionContext.class,
            ImmutableTransactionContext.builder().build())
        ).build();

    assertThat(fetcher.getScopes(context)).hasSize(1).containsOnly("test_assetalias");
  }

  @Test
  public void testReturnsMultipleScopesForClientsWithRules() {
    final AuthorizationFetcher fetcher = new AuthorizationFetcher(
        Stream.of("test1_oauth2", "test2_oauth2").collect(Collectors.toSet()));

    MockServerRequest serverRequest = MockServerRequest.builder()
        .header(AUTHZ_SCOPE, "test1_oauth2,test2_oauth2,test3_oauth2")
        .build();

    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .of(Context.class, Context.of(TransactionContext.class,
            ImmutableTransactionContext.builder().build())
        ).build();

    assertThat(fetcher.getScopes(context)).hasSize(2).contains("test1_oauth2", "test2_oauth2");
  }
}
