package com.intuit.graphql.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties("authz")
@Data
public class AuthZProperties {

  private boolean enabled = false;
  private boolean logUnauthorized = false;

  private String versionsFileName = "versions.yml";
  private String rulesDirectory = "authz-rules";
}
