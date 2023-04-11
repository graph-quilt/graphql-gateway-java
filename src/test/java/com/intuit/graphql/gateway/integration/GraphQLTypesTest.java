package com.intuit.graphql.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GraphQLTypesTest extends GraphQLTestCase {

  @Test
  public void graphHasStandardAndSpecTypes() {
    String query = getQuery("baseTypesQuery.graphql");
    String wiremockResponse = getResponse("baseTypesQueryResponse.json");

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
              .extracting("standardTypes")
              .extracting(
                  "int",
                  "float",
                  "string",
                  "boolean",
                  "id"
//                  "bigDecimal",
//                  "bigInt",
//                  "short",
//                  "long",
//                  "char"
              ).containsOnly(
              1234,
              1234.56,
              "aabbcc",
              true,
              "aabbcc"
//              new BigDecimal("12345.67"),
//              new BigInteger("123456"),
//              new Short("1"),
//              1234L,
//              'a'
          );
        });
  }
}
