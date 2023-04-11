package com.intuit.graphql.gateway.metrics;

import com.intuit.graphql.gateway.config.properties.ExecutionMetricsProperties;
import graphql.execution.instrumentation.Instrumentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExecutionMetricsManager {

  private final ExecutionMetricsProperties executionMetricsProperties;

  public ExecutionMetricsManager(final ExecutionMetricsProperties executionMetricsProperties) {
    this.executionMetricsProperties = executionMetricsProperties;
  }

  public boolean isExecutionMetricsEnabled() {
    return executionMetricsProperties.isEnabled();
  }

  public Instrumentation getInstrumentation() {
    return new ExecutionMetricsInstrumentation();
  }

}