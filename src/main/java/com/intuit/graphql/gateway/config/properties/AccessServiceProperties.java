package com.intuit.graphql.gateway.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
@ConfigurationProperties("access.service")
@Getter
@Setter
public class AccessServiceProperties {

  private String offlineTicketUrl;
}
