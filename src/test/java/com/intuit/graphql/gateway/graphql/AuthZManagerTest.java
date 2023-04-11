package com.intuit.graphql.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.authorization.AuthZLogListener;
import com.intuit.graphql.gateway.config.properties.AuthZProperties;
import com.intuit.graphql.gateway.s3.S3RulePackage;
import com.intuit.graphql.gateway.s3.S3RuleRegistry;
import com.intuit.graphql.gateway.s3.S3RuleRegistry.S3RulesChangedEvent;
import com.intuit.graphql.gateway.s3.RuleConfig;
import com.intuit.graphql.gateway.s3.RuleConfig.RuleType;
import com.intuit.graphql.gateway.s3.S3Rule;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import graphql.execution.instrumentation.Instrumentation;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AuthZManagerTest {

  @Mock
  public SchemaManager schemaManager;

  public AuthZProperties authZProperties;

  @Mock
  public S3RuleRegistry registry;

  @Mock
  public AuthZLogListener authZLogListener;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    authZProperties = new AuthZProperties();

    final RuleConfig ruleConfig =  RuleConfig.builder()
        .id("test")
        .description("test")
        .type(RuleType.OFFLINE).build();
    S3Rule rules = S3Rule.builder()
        .ruleConfig(ruleConfig)
        .queries(Collections.emptyList())
        .build();
    when(schemaManager.getRuntimeGraph()).thenReturn(RuntimeGraph.emptyGraph());
    when(registry.get()).thenReturn(S3RulePackage.builder()
        .authzRulesById(Collections.singletonMap("test-key", rules))
        .build());
  }

  @Test
  public void buildsAuthZInstrumentation() {
    authZProperties.setEnabled(true);
    AuthZManager manager = new AuthZManager(authZProperties, schemaManager, registry, authZLogListener);

    manager.buildAuthZInstrumentation();

    final Instrumentation result = manager.getInstrumentation();
    assertThat(result).isNotNull();
  }

  @Test
  public void doesNotBuildInstrumentationIfAuthZDisabled() {
    authZProperties.setEnabled(false);

    new AuthZManager(authZProperties, schemaManager, registry, authZLogListener).buildAuthZInstrumentation();

    verify(schemaManager, Mockito.never()).getRuntimeGraph();
  }

  @Test
  public void rebuildSchemaOnApplicationEvent() {

    authZProperties.setEnabled(true);

    AuthZManager manager = new AuthZManager(authZProperties, schemaManager, registry, authZLogListener);

    manager.onAuthZRulesChangedEvent(S3RulesChangedEvent.INSTANCE);

    final Instrumentation result = manager.getInstrumentation();

    assertThat(result).isNotNull();
  }
}
