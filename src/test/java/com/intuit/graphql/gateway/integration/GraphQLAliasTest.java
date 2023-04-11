package com.intuit.graphql.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GraphQLAliasTest extends GraphQLTestCase {

  @Test
  public void returnsAliasField() {

    String query = getQuery("aliasQuery.graphql");
    String wiremockResponse = getResponse("aliasQueryResponse.json");

    TestQuery testQuery = TestQuery.newQuery()
        .query(query)
        .build();

    this.addGraphQLResponse(stubFor(post("/standardtypes")
        .withRequestBody(containing("standardTypes"))
        .willReturn(aResponse().withBody(wiremockResponse)
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
        ))
    );

    this.runTestQuery(testQuery)
        .thenAssert(executionResult -> {
          assertThat(executionResult.getErrors()).isEmpty();
          assertThat(executionResult.toSpecification())
              .containsOnlyKeys("data")
              .extracting("data")
              .extracting("aliasStandardTypes")
              .extracting("aliasInt")
              .isInstanceOf(Integer.class)
              .isEqualTo(1234);

        });
  }
}
