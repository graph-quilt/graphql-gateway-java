package com.intuit.graphql.gateway.router;

import static com.intuit.graphql.gateway.router.HealthRouter.healthRoute;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

public class HealthRouterTest {

  private static MockServerHttpRequest mockInvalidRequest;
  private static MockServerHttpRequest mockValidRequest;

  private static MockServerRequest invalidRequest;
  private static MockServerRequest validRequest;

  @BeforeClass
  public static void setupOnce() {
    mockInvalidRequest = MockServerHttpRequest.get("https://example.com/health/local").build();
    mockValidRequest = MockServerHttpRequest.post("https://example.com/health/local").build();

    invalidRequest = MockServerRequest.builder()
        .uri(URI.create("/health/local"))
        .exchange(MockServerWebExchange.from(mockInvalidRequest))
        .method(HttpMethod.POST)
        .build();

    validRequest = MockServerRequest.builder()
        .uri(URI.create("/health/local"))
        .exchange(MockServerWebExchange.from(mockValidRequest))
        .method(HttpMethod.GET)
        .build();
  }

  @Test
  public void testRoute() {
    assertThat(healthRoute.test(invalidRequest)).isFalse();
    assertThat(healthRoute.test(validRequest)).isTrue();
  }
}
