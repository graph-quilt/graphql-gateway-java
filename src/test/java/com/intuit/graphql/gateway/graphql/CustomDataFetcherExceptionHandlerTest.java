package com.intuit.graphql.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.webclient.TxProvider;
import graphql.GraphQLContext;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.util.context.Context;

public class CustomDataFetcherExceptionHandlerTest {

  @Mock
  private DataFetcherExceptionHandlerParameters p;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doReturn(new SourceLocation(1, 1))
        .when(p).getSourceLocation();
    doReturn(mock(ExecutionPath.class))
        .when(p).getPath();
  }

  @Test
  public void addsErrorToExecutionContext() {

    CustomDataFetcherExceptionHandler f = new CustomDataFetcherExceptionHandler();
    DataFetchingEnvironment dfe = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .context(
            GraphQLContext.newContext().of(Context.class, Context.of(TransactionContext.class, TxProvider.emptyTx())))
        .build();

    when(p.getException()).thenReturn(new RuntimeException("boom"));
    when(p.getDataFetchingEnvironment()).thenReturn(dfe);

    final DataFetcherExceptionHandlerResult result = f.onException(p);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void doesNotThrowErrorIfTxMissing() {
    CustomDataFetcherExceptionHandler f = new CustomDataFetcherExceptionHandler();

    when(p.getException()).thenReturn(new DataRetrieverException("boom"));

    final DataFetcherExceptionHandlerResult result = f.onException(p);
    assertThat(result.getErrors()).isNotEmpty();

  }

  @Test
  public void unwrapsCompletionExceptions() {
    CustomDataFetcherExceptionHandler f = new CustomDataFetcherExceptionHandler();

    when(p.getException()).thenReturn(new CompletionException(new DataRetrieverException("boom")));

    final DataFetcherExceptionHandlerResult result = f.onException(p);

    assertThat(result.getErrors()).extracting(GraphQLError::getMessage)
        .doesNotContain("DataRetrieverException");
  }

  @Test
  public void doesNotUnwrapIfCauseIsNull() {
    CustomDataFetcherExceptionHandler f = new CustomDataFetcherExceptionHandler();

    when(p.getException()).thenReturn(new DataRetrieverException("boom"));

    final DataFetcherExceptionHandlerResult result = f.onException(p);

    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
  }
}