package com.intuit.graphql.gateway.graphql;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.introspection.IntrospectionManager;
import com.intuit.graphql.gateway.metrics.ExecutionMetricsManager;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.ExecutionInput;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataApiGraphQLExecutorTest {

  @Mock
  SchemaManager schemaManager;

  @Mock
  AuthZManager authZManager;

  @Mock
  ExecutionMetricsManager executionMetricsManager;

  @Mock
  DataFetcherExceptionHandler exceptionHandler;

  @Mock
  IntrospectionManager introspectionManager;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    when(schemaManager.getRuntimeGraph()).thenReturn(SchemaStitcher.newBuilder().build().stitchGraph());
  }


  @Test
  public void canRunEmptyExecutionTest() {
    GraphqlGatewayExecutor executor = new GraphqlGatewayExecutor(schemaManager, authZManager,
        introspectionManager, executionMetricsManager, exceptionHandler);
    executor.execute(ExecutionInput.newExecutionInput().query("").build());
  }


  @Test
  public void addsIntrospectionInstrumentation() {

    Instrumentation mockInstrumentation = spy(SimpleInstrumentation.class);
    when(introspectionManager.isIntrospectionNotEnabled()).thenReturn(true);
    when(introspectionManager.getInstrumentation()).thenReturn(mockInstrumentation);

    GraphqlGatewayExecutor executor = new GraphqlGatewayExecutor(schemaManager, authZManager,
        introspectionManager, executionMetricsManager, exceptionHandler);

    executor.execute(ExecutionInput.newExecutionInput().query("").build());

    verify(introspectionManager, times(1)).getInstrumentation();
  }

  @Test
  public void doesNotAddIntrospectionInstrumentationIfDisabled() {

    Instrumentation mockInstrumentation = spy(SimpleInstrumentation.class);
    when(introspectionManager.isIntrospectionNotEnabled()).thenReturn(false);
    when(introspectionManager.getInstrumentation()).thenReturn(mockInstrumentation);

    GraphqlGatewayExecutor executor = new GraphqlGatewayExecutor(schemaManager, authZManager,
        introspectionManager, executionMetricsManager, exceptionHandler);

    executor.execute(ExecutionInput.newExecutionInput().query("").build());

    verify(introspectionManager, never()).getInstrumentation();
  }

  @Test
  public void addsExecutionMetricsInstrumentation() {
    Instrumentation mockInstrumentation = spy(SimpleInstrumentation.class);
    when(executionMetricsManager.isExecutionMetricsEnabled()).thenReturn(true);
    when(executionMetricsManager.getInstrumentation()).thenReturn(mockInstrumentation);

    GraphqlGatewayExecutor executor = new GraphqlGatewayExecutor(schemaManager, authZManager,
        introspectionManager, executionMetricsManager, exceptionHandler);

    executor.execute(ExecutionInput.newExecutionInput().query("").build());

    verify(executionMetricsManager, times(1)).getInstrumentation();
  }

  @Test
  public void doesNotAddExecutionMetricsInstrumentation() {

    Instrumentation mockInstrumentation = spy(SimpleInstrumentation.class);
    when(executionMetricsManager.isExecutionMetricsEnabled()).thenReturn(false);
    when(executionMetricsManager.getInstrumentation()).thenReturn(mockInstrumentation);

    GraphqlGatewayExecutor executor = new GraphqlGatewayExecutor(schemaManager, authZManager,
        introspectionManager, executionMetricsManager, exceptionHandler);

    executor.execute(ExecutionInput.newExecutionInput().query("").build());
    verify(executionMetricsManager, never()).getInstrumentation();
  }

}
