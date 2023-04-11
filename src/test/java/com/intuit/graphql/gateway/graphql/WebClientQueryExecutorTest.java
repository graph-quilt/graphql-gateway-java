package com.intuit.graphql.gateway.graphql;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.intuit.graphql.gateway.metrics.DownstreamCallEvent;
import com.intuit.graphql.gateway.metrics.ExecutionMetrics.ExecutionMetricsData;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

@RunWith(MockitoJUnitRunner.class)
public class WebClientQueryExecutorTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);
  private final WebClient webClient = WebClient.create();

  @Mock
  private ExecutionMetricsData mockExecutionMetricsData;


  @Test(expected = ServiceRegistrationException.class)
  public void constructorThrowsIllegalArgumentException() {
    ServiceDefinition serviceDefinition = ServiceDefinition.newBuilder().namespace("test").type(Type.GRAPHQL).build();
    new WebClientQueryExecutor(null, serviceDefinition);
  }

  @Test
  public void queryReturnsDataRetrieverException() {
    wireMockRule.addStubMapping(stubFor(post("/").willReturn(aResponse().withStatus(500))));
    ServiceDefinition serviceDefinition = ServiceDefinition.newBuilder().namespace("test").type(Type.GRAPHQL)
        .endpoint("localhost:" + wireMockRule.port()).build();

    final WebClientQueryExecutor webClientQueryExecutor = new WebClientQueryExecutor(webClient, serviceDefinition);

    ExecutionInput i = ExecutionInput.newExecutionInput()
        .query("")
        .variables(new HashedMap<>())
        .build();

    GraphQLContext graphQLContext = GraphQLContext.newContext()
        .of(Context.class, Context.empty())
        .of(ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    StepVerifier.create(
        Mono.fromFuture(
            webClientQueryExecutor.query(i, graphQLContext)))
        .expectError(DataRetrieverException.class).verify();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));
  }

  @Test
  public void issuesValidRequest() {
    wireMockRule.addStubMapping(
        stubFor(post("/")
            .withHeader("Content-Type", equalTo(APPLICATION_JSON))
            .willReturn(aResponse()
                .withHeader("Content-Type", APPLICATION_JSON)
                .withStatus(200)
                .withBody("{\"a\": 1}"))
        )
    );
    ServiceDefinition serviceDefinition = ServiceDefinition.newBuilder().namespace("test").type(Type.GRAPHQL)
        .endpoint("localhost:" + wireMockRule.port()).build();

    final WebClientQueryExecutor webClientQueryExecutor = new WebClientQueryExecutor(webClient, serviceDefinition);

    ExecutionInput i = ExecutionInput.newExecutionInput()
        .query("")
        .variables(new HashMap<>())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext().of(Context.class, Context.empty())
        .of(ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    StepVerifier.create(
        Mono.fromFuture(
            webClientQueryExecutor.query(i, graphQLContext)))
        .consumeNextWith(map -> assertThat(map.get("a")).isEqualTo(1))
        .verifyComplete();

    Mockito.verify(mockExecutionMetricsData, times(1))
        .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));
  }

  @Test
  public void queryReturnsTimeoutException() {
    wireMockRule.addStubMapping(
        stubFor(post("/")
            .withHeader("Content-Type", equalTo(APPLICATION_JSON))
            .willReturn(aResponse().withFixedDelay(1001)
                .withHeader("Content-Type", APPLICATION_JSON)
                .withStatus(200)
                .withBody("{\"a\": 1}"))
        )
    );
    ServiceDefinition serviceDefinition = ServiceDefinition.newBuilder().namespace("test").timeout(1000).type(Type.GRAPHQL)
        .endpoint("localhost:" + wireMockRule.port()).build();

    final WebClientQueryExecutor webClientQueryExecutor = new WebClientQueryExecutor(webClient, serviceDefinition);

    ExecutionInput i = ExecutionInput.newExecutionInput()
        .query("")
        .variables(new HashMap<>())
        .build();

    GraphQLContext graphQLContext = GraphQLContext
        .newContext().of(Context.class, Context.empty())
        .of(ExecutionMetricsData.class, mockExecutionMetricsData)
        .build();

    StepVerifier.create(
        Mono.fromFuture(
            webClientQueryExecutor.query(i, graphQLContext)))
        .expectError(TimeoutException.class).verify();

    // TODO Comment out in the future.  see DOS-1291
    //   doOnError and doFinally are both called in this test. doFinally is skipped in actual.
    //   Mockito.verify(mockExecutionMetricsData, times(1))
    //     .addDownstreamCallEvent(ArgumentMatchers.any(DownstreamCallEvent.class));
  }

}