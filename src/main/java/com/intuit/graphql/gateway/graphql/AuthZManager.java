package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.authorization.AuthZLogListener;
import com.intuit.graphql.gateway.authorization.AuthorizationFetcher;
import com.intuit.graphql.gateway.config.properties.AuthZProperties;
import com.intuit.graphql.gateway.events.GraphQLSchemaChangedEvent;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.s3.S3RuleRegistry;
import com.intuit.graphql.gateway.s3.S3RuleRegistry.S3RulesChangedEvent;
import com.intuit.graphql.gateway.s3.RuleConfig.RuleType;
import com.intuit.graphql.gateway.s3.S3Rule;
import com.intuit.graphql.gateway.s3.RuleConfig;
import com.intuit.graphql.authorization.config.AuthzClient;
import com.intuit.graphql.authorization.config.AuthzClient.ClientAuthorizationType;
import com.intuit.graphql.authorization.enforcement.AuthzInstrumentation;
import com.intuit.graphql.authorization.util.PrincipleFetcher;
import graphql.execution.instrumentation.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Manages the caching and lifecycle of AuthZInstrumentation for Data API. This class exposes methods that allow dynamic
 * runtime configurations and polling to trigger re-builds of the AuthZInstrumentation.
 */
@Component
@Slf4j
public class AuthZManager {

  private final AuthZProperties authzProperties;
  private final SchemaManager schemaManager;
  private final S3RuleRegistry s3RuleRegistry;
  private final AuthZLogListener authZLogListener;

  private volatile Instrumentation authZInstrumentation = null;

  public AuthZManager(final AuthZProperties authzProperties,
      final SchemaManager schemaManager,
      final S3RuleRegistry s3RuleRegistry,
      final AuthZLogListener authZLogListener
  ) {
    this.authzProperties = authzProperties;
    this.schemaManager = schemaManager;
    this.s3RuleRegistry = s3RuleRegistry;
    this.authZLogListener = authZLogListener;
  }

  @PostConstruct
  public void buildAuthZInstrumentation() {
    if (this.isAuthZEnabled()) {
      this.authZInstrumentation = buildS3AuthZInstrumentation();
    } else {
      this.authZInstrumentation = null;
    }
  }

  public boolean isAuthZEnabled() {
    return authzProperties.isEnabled();
  }

  public Instrumentation getInstrumentation() {
    return this.authZInstrumentation;
  }

  private AuthzInstrumentation buildS3AuthZInstrumentation() {
    final Map<AuthzClient, List<String>> rules = this.s3RuleRegistry.get().getAuthzRulesById().values().stream()
        .filter(authzRules -> authzRules.getRuleConfig().getType() == RuleType.OFFLINE)
        .collect(Collectors.toMap(authZRules -> toAuthzClient(authZRules.getRuleConfig()), S3Rule::getQueries));
    try {
      Set<String> clients = rules.keySet().stream()
          .map(AuthzClient::getId)
          .collect(Collectors.toSet());

      PrincipleFetcher fetcher = new AuthorizationFetcher(clients);
      return new AuthzInstrumentation(() -> rules, this.schemaManager.getRuntimeGraph().getExecutableSchema(), fetcher,
          this.authZLogListener);
    } catch (Exception e) {
      EventLogger.error(log, null, "Failed to build AuthZInstrumentation", e);
      return null;
    }
  }

  public static AuthzClient toAuthzClient(RuleConfig ruleConfig){
    final AuthzClient authzClient = new AuthzClient();
    authzClient.setId(ruleConfig.getId());
    authzClient.setType(ClientAuthorizationType.OFFLINE);
    authzClient.setDescription(ruleConfig.getDescription());
    return authzClient;
  }

  @EventListener
  public void onSchemaChangeEvent(GraphQLSchemaChangedEvent event) {
    log.info("Received GraphQLSchemaChangeEvent, rebuilding AuthZInstrumentation");
    buildAuthZInstrumentation();
  }

  @EventListener
  public void onAuthZRulesChangedEvent(final S3RulesChangedEvent event) {
    log.info("Received AuthZRulesChangedEvent, rebuilding AuthZInstrumentation");
    buildAuthZInstrumentation();
  }
}
