package com.intuit.graphql.gateway.provider;

import com.intuit.graphql.gateway.graphql.DataRetrieverException;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import graphql.GraphQLContext;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.junit.Test;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.context.Context;

public class ServiceProviderHelperTest {

  @Test(expected = NullPointerException.class)
  public void validateThrowsExceptionOnMissingReactorContext() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().build();
    GraphQLContext context = GraphQLContext.newContext()
        .build();
    ServiceProviderHelper.validate(sd, context);
  }

  @Test(expected = NoSuchElementException.class)
  public void validateThrowsExceptionOnMissingTransactionContext() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().build();
    GraphQLContext context = GraphQLContext.newContext()
        .of(Context.class, Context.empty())
        .build();
    ServiceProviderHelper.validate(sd, context);
  }

  @Test(expected = NullPointerException.class)
  public void validateThrowsExceptionOnMissingServerRequest() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().build();
    GraphQLContext context = GraphQLContext.newContext()
        .of(Context.class, Context.of(TransactionContext.class,
            ImmutableTransactionContext.builder().build())
        ).build();
    ServiceProviderHelper.validate(sd, context);
  }

  @Test
  public void validatePassThroughOnEmptyWhiteList() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().build();
    MockServerRequest serverRequest = MockServerRequest.builder().build();
    TransactionContext tx = ImmutableTransactionContext.builder().build();
    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .of(Context.class, Context.of(TransactionContext.class, tx))
        .build();
    ServiceProviderHelper.validate(sd, context);
  }

  @Test(expected = DataRetrieverException.class)
  public void validateThrowsExceptionOnMissingAppIdForWhiteListedService() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().clientWhitelist(Collections.singleton("test-client")).build();
    MockServerRequest serverRequest = MockServerRequest.builder().build();
    TransactionContext tx = ImmutableTransactionContext.builder().build();
    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .of(Context.class, Context.of(TransactionContext.class, tx))
        .build();
    ServiceProviderHelper.validate(sd, context);
  }

  @Test(expected = DataRetrieverException.class)
  public void validateThrowsExceptionIfClientIsNotWhiteListed() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().clientWhitelist(Collections.singleton("test-client")).build();
    MockServerRequest serverRequest = MockServerRequest.builder().build();
    TransactionContext tx = ImmutableTransactionContext.builder().appId("test").build();
    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .of(Context.class, Context.of(TransactionContext.class, tx))
        .build();
    ServiceProviderHelper.validate(sd, context);
  }

  @Test
  public void validatePassesOnWhiteListedClient() {
    ServiceDefinition sd = ServiceDefinition.newBuilder().clientWhitelist(Collections.singleton("test-client")).build();
    MockServerRequest serverRequest = MockServerRequest.builder().build();
    TransactionContext tx = ImmutableTransactionContext.builder().appId("test-client")
        .build();
    GraphQLContext context = GraphQLContext.newContext()
        .of(ServerRequest.class, serverRequest)
        .of(Context.class, Context.of(TransactionContext.class, tx))
        .build();
    ServiceProviderHelper.validate(sd, context);
  }
}
