package com.intuit.graphql.gateway.router;


import static com.intuit.graphql.gateway.router.VisualizerRouter.visualizerRoute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.net.URI;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

public class VisualizerRouterTest {

  private static MockServerHttpRequest mockSimpleGetRequest;
  private static MockServerHttpRequest mockSimplePostRequest;
  private static MockServerHttpRequest mockPostRequest;

  private static MockServerRequest simpleGetRequest;
  private static MockServerRequest simplePostRequest;
  private static MockServerRequest postRequest;

  @BeforeClass
  public static void setupOnce() {
    mockSimpleGetRequest = MockServerHttpRequest.get("https://example.com/graphiql/visualizer").build();
    mockSimplePostRequest = MockServerHttpRequest.post("https://example.com/graphiql/visualizer").build();
    mockPostRequest = MockServerHttpRequest
        .post("https://example.com/dev/graphiql/visualizer")
        .build();

    simpleGetRequest = MockServerRequest.builder()
        .uri(URI.create("/graphiql/visualizer"))
        .exchange(MockServerWebExchange.from(mockSimpleGetRequest))
        .method(HttpMethod.GET)
        .build();

    simplePostRequest = MockServerRequest.builder()
        .uri(URI.create("/graphiql/visualizer"))
        .exchange(MockServerWebExchange.from(mockSimplePostRequest))
        .method(HttpMethod.POST)
        .build();

    postRequest = MockServerRequest.builder()
        .uri(URI.create("/graphiql/visualizer"))
        .exchange(MockServerWebExchange.from(mockPostRequest))
        .method(HttpMethod.POST)
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Test
  public void testRoute() {
    assertThat(visualizerRoute.test(simpleGetRequest)).isTrue();
    assertThat(visualizerRoute.test(simplePostRequest)).isFalse();
    assertThat(visualizerRoute.test(postRequest)).isFalse();
  }
}