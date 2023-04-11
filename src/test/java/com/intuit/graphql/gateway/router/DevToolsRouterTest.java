package com.intuit.graphql.gateway.router;

import static com.intuit.graphql.gateway.router.DevToolsRouter.GRAPHQL_PROVIDER_ROUTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

public class DevToolsRouterTest {

  private static MockServerHttpRequest mockRequest;
  private static MockServerRequest mockServerRequest;

  @BeforeClass
  public static void setupOnce() {
    mockRequest = MockServerHttpRequest.get("https://example.com/dev/graphql/providers").build();
    mockServerRequest = MockServerRequest.builder()
        .uri(URI.create("/dev/graphql/providers"))
        .exchange(MockServerWebExchange.from(mockRequest))
        .method(HttpMethod.GET)
        .build();
  }

  @Test
  public void testRequestPredicate() {
    assertThat(
        GRAPHQL_PROVIDER_ROUTE.test(mockServerRequest))
        .isTrue();
  }

  @Test
  public void testRoute() {
    assertThat(GRAPHQL_PROVIDER_ROUTE.test(mockServerRequest)).isTrue();

  }

}