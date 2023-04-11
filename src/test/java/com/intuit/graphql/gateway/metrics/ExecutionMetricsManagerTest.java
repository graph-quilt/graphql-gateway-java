package com.intuit.graphql.gateway.metrics;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.config.properties.ExecutionMetricsProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionMetricsManagerTest {

  @Mock
  ExecutionMetricsProperties executionMetricsPropertiesMock;

  private ExecutionMetricsManager subject;

  @Before
  public void setup() {
     subject = new ExecutionMetricsManager(executionMetricsPropertiesMock);
  }

  @Test
  public void isExecutionMetricsEnabled_configuredTrue_returnsTrue() {
    when(executionMetricsPropertiesMock.isEnabled()).thenReturn(true);
    assertThat(subject.isExecutionMetricsEnabled()).isTrue();
  }

  @Test
  public void isExecutionMetricsEnabled_configuredFalse_returnsFalse() {
    when(executionMetricsPropertiesMock.isEnabled()).thenReturn(false);
    assertThat(subject.isExecutionMetricsEnabled()).isFalse();
  }

  @Test
  public void getInstrumentation_returnsExecutionMetricsInstrumentationObject() {
    assertThat(subject.getInstrumentation()).isInstanceOf(ExecutionMetricsInstrumentation.class);
  }


}
