package com.intuit.graphql.gateway.introspection;

import graphql.schema.DataFetchingEnvironment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbortIntrospectionDataFetcherTest {

  private AbortIntrospectionDataFetcher subject = new AbortIntrospectionDataFetcher();

  @Mock
  DataFetchingEnvironment dataFetchingEnvironmentMock;

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void get_always_throws_IntrospectionAbortedGraphQLError() throws Exception {

    expectedEx.expect(IntrospectionAbortedGraphQLError.class);
    expectedEx.expectMessage("Introspection aborted.");

    subject.get(dataFetchingEnvironmentMock);


  }

}
