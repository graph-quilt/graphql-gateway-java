package com.intuit.graphql.gateway.router;


import static com.intuit.graphql.gateway.router.GraphQLRouter.graphQLRoute;
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

public class GraphQLRouterTest {

  private static MockServerHttpRequest mockSimpleGetRequest;
  private static MockServerHttpRequest mockSimplePostRequest;
  private static MockServerHttpRequest mockPostRequest;

  private static MockServerRequest simpleGetRequest;
  private static MockServerRequest postWithoutContentType;
  private static MockServerRequest validRequest;

  @BeforeClass
  public static void setupOnce() {
    mockSimpleGetRequest = MockServerHttpRequest.get("https://example.com/graphql").build();
    mockSimplePostRequest = MockServerHttpRequest.post("https://example.com/graphql").build();
    mockPostRequest = MockServerHttpRequest
        .post("https://example.com/dev/graphql")
        .build();

    simpleGetRequest = MockServerRequest.builder()
        .uri(URI.create("/graphql"))
        .exchange(MockServerWebExchange.from(mockSimpleGetRequest))
        .method(HttpMethod.GET)
        .build();

    postWithoutContentType = MockServerRequest.builder()
        .uri(URI.create("/graphql"))
        .exchange(MockServerWebExchange.from(mockSimplePostRequest))
        .method(HttpMethod.POST)
        .build();

    validRequest = MockServerRequest.builder()
        .uri(URI.create("/graphql"))
        .exchange(MockServerWebExchange.from(mockPostRequest))
        .method(HttpMethod.POST)
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header(ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Test
  public void testRoute() {
    assertThat(graphQLRoute.test(simpleGetRequest)).isFalse();
    assertThat(graphQLRoute.test(postWithoutContentType)).isFalse();
    assertThat(graphQLRoute.test(validRequest)).isTrue();
  }
}