package com.intuit.graphql.gateway.metrics;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.metrics.ExecutionMetrics.ExecutionMetricsData;
import com.intuit.graphql.gateway.metrics.ExecutionMetricsInstrumentation.ExecutionMetricsInstrumentationState;
import com.intuit.graphql.gateway.webclient.TxProvider;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.util.context.Context;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionMetricsInstrumentationTest {

  @Mock
  Context contextMock;

  @Mock
  InstrumentationCreateStateParameters parametersMock;

  @Mock
  ExecutionContext executionContextMock;

  @Mock
  InstrumentationExecutionParameters instrumentationExecutionParametersMock;

  @Mock
  GraphQLContext graphQLContextMock;

  ExecutionMetricsInstrumentation subject = new ExecutionMetricsInstrumentation();

  @Test
  public void createState_success() {
    InstrumentationState actual = subject.createState(parametersMock);
    assertThat(actual).isInstanceOf(ExecutionMetricsInstrumentationState.class);
  }

  @Test
  public void instrumentExecutionContext_success() {
    ExecutionMetricsData executionMetricsDataMock = mock(ExecutionMetricsData.class);
    ExecutionMetricsInstrumentationState instrumentationState = mock(ExecutionMetricsInstrumentationState.class);
    when(instrumentationExecutionParametersMock.getInstrumentationState()).thenReturn(instrumentationState);
    when(instrumentationExecutionParametersMock.getContext()).thenReturn(graphQLContextMock);
    when(instrumentationState.getExecutionMetricsData()).thenReturn(executionMetricsDataMock);

    ExecutionContext actual = subject.instrumentExecutionContext(executionContextMock, instrumentationExecutionParametersMock);

    assertThat(actual).isNotNull();
    verify(graphQLContextMock, times(1)).put(eq(ExecutionMetricsData.class),any(ExecutionMetricsData.class));
  }

  @Test
  public void instrumentExecutionResult_success() {
    when(contextMock.getOrEmpty(TransactionContext.class)).thenReturn(Optional.of(TxProvider.emptyTx()));
    when(graphQLContextMock.getOrEmpty(Context.class)).thenReturn(Optional.of(contextMock));
    ExecutionResult executionResultMock = mock(ExecutionResult.class);
    when(instrumentationExecutionParametersMock.getContext()).thenReturn(graphQLContextMock);
    ExecutionMetricsInstrumentationState instrumentationStateMock = mock(ExecutionMetricsInstrumentationState.class);
    when(instrumentationExecutionParametersMock.getInstrumentationState()).thenReturn(instrumentationStateMock);
    ExecutionMetricsData executionMetricsDataMock = mock(ExecutionMetricsData.class);
    when(instrumentationStateMock.getExecutionMetricsData()).thenReturn(executionMetricsDataMock);

    CompletableFuture<ExecutionResult> actual = subject
        .instrumentExecutionResult(executionResultMock, instrumentationExecutionParametersMock);

    assertThat(actual).isNotNull();
    verify(executionMetricsDataMock, times(1)).getDownstreamCallEvents();
  }

}
