package com.intuit.graphql.gateway.introspection;

import com.intuit.graphql.gateway.config.properties.IntrospectionProperties;
import graphql.execution.instrumentation.Instrumentation;
import org.springframework.stereotype.Component;

@Component
public class IntrospectionManager {

  private final IntrospectionProperties instrumentationProperties;

  public IntrospectionManager(final IntrospectionProperties properties) {
    this.instrumentationProperties = properties;
  }

  public boolean isIntrospectionNotEnabled() {
    return !this.instrumentationProperties.isEnabled();
  }

  public Instrumentation getInstrumentation() {
    return new IntrospectionInstrumentation();
  }

}
