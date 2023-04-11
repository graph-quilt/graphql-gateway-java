package com.intuit.graphql.gateway.introspection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.webclient.TxProvider;
import graphql.GraphQLContext;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.util.context.Context;

@RunWith(MockitoJUnitRunner.class)
public class IntrospectionInstrumentationTest {

  private IntrospectionInstrumentation subject = new IntrospectionInstrumentation();

  @Mock
  private DataFetcher originalDataFetcherMock;

  @Mock
  private InstrumentationFieldFetchParameters instrumentationFieldFetchParametersMock;

  @Mock
  private DataFetchingEnvironment dataFetchingEnvironmentMock;

  @Mock
  private Context contextMock;

  @Mock
  private GraphQLContext graphQLContextMock;

  @Mock
  private TransactionContext transactionContextMock;

  @Mock
  private GraphQLSchema graphQLSchemaMock;

  @Mock
  private GraphQLObjectType queryTypeMock;

  @Mock
  private Field fieldMock;

  @Before
  public void setup() {
    when(queryTypeMock.getName()).thenReturn("Query");

    when(graphQLSchemaMock.getQueryType()).thenReturn(queryTypeMock);

    when(graphQLContextMock.get(Context.class)).thenReturn(contextMock);
    when(contextMock.get(TransactionContext.class)).thenReturn(TxProvider.emptyTx());

    when(dataFetchingEnvironmentMock.getContext()).thenReturn(graphQLContextMock);
    when(dataFetchingEnvironmentMock.getGraphQLSchema()).thenReturn(graphQLSchemaMock);

    when(instrumentationFieldFetchParametersMock.getEnvironment()).thenReturn(dataFetchingEnvironmentMock);
  }

  @Test
  public void instrumentDataFetcher_notSchemaIntrospection_returnsOriginalDataFetcher() {
    when(fieldMock.getName()).thenReturn("someValidTopLevelField");
    when(dataFetchingEnvironmentMock.getField()).thenReturn(fieldMock);
    when(dataFetchingEnvironmentMock.getParentType()).thenReturn(queryTypeMock);

    DataFetcher actual = subject.instrumentDataFetcher(originalDataFetcherMock, instrumentationFieldFetchParametersMock);

    assertThat(actual).isSameAs(originalDataFetcherMock);

  }

  @Test
  public void instrumentDataFetcher_isASchemaIntrospection_returnsAbortOriginalDataFetcher() {
    when(fieldMock.getName()).thenReturn("__schema");
    when(dataFetchingEnvironmentMock.getField()).thenReturn(fieldMock);
    when(dataFetchingEnvironmentMock.getParentType()).thenReturn(queryTypeMock);

    DataFetcher actual = subject.instrumentDataFetcher(originalDataFetcherMock, instrumentationFieldFetchParametersMock);

    assertThat(actual).isInstanceOf(AbortIntrospectionDataFetcher.class);

  }

  @Test
  public void instrumentDataFetcher_isTypeIntrospection_returnsAbortOriginalDataFetcher() {
    when(fieldMock.getName()).thenReturn("__type");
    when(dataFetchingEnvironmentMock.getField()).thenReturn(fieldMock);
    when(dataFetchingEnvironmentMock.getParentType()).thenReturn(queryTypeMock);

    DataFetcher actual = subject.instrumentDataFetcher(originalDataFetcherMock, instrumentationFieldFetchParametersMock);

    assertThat(actual).isSameAs(originalDataFetcherMock);

  }

}
