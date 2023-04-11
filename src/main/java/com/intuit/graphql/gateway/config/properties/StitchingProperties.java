package com.intuit.graphql.gateway.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
@ConfigurationProperties(StitchingProperties.CONFIG_PREFIX)
@Getter
@Setter
public class StitchingProperties {

  public static final String CONFIG_PREFIX = "stitching";
  public static final String CONFIG_REBUILD = "rebuild";

  /**
   * Toggle flag to rebuild schema.
   */
  private boolean rebuild = false;
}
