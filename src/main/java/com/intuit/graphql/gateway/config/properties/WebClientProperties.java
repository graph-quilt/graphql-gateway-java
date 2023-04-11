package com.intuit.graphql.gateway.config.properties;

import static com.intuit.graphql.gateway.config.properties.WebClientProperties.CONFIG_PREFIX;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(CONFIG_PREFIX)
@Data
public class WebClientProperties {
  public static final String CONFIG_PREFIX = "webclient";
  private int timeout = 10000;
  private int connectTimeout = 2000;
  private int codecMaxInMemorySizeInMbytes = 16;
}
