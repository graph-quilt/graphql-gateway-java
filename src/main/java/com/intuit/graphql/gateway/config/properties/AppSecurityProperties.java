package com.intuit.graphql.gateway.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Data
@Configuration
@ConfigurationProperties("app.security")
public class AppSecurityProperties {

  @Autowired
  private Environment env;
  private String appId;
  private String appSecret;

}
