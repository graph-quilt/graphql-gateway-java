package com.intuit.graphql.gateway.config.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties("consent.service")
@Data
public class ConsentServiceClientProperties {
  private String uri;
  private long timeout= 500;
}
