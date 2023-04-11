package com.intuit.graphql.gateway.config.properties;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("app.logging")
public class AppLoggingProperties {

  private String appId = "";
  private String env = "UNKNOWN";
  private String version = "UNKNOWN";
  private int order = HIGHEST_PRECEDENCE;
}
