package com.intuit.graphql.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties("metrics.execution")
@Data
public class ExecutionMetricsProperties {
  private boolean enabled;
}
