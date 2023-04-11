package com.intuit.graphql.gateway.config.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@RefreshScope
@Configuration
@ConfigurationProperties(RegistrationBreakingChangeProperties.CONFIG_PREFIX)
@Data
@Getter
@Setter
public class RegistrationBreakingChangeProperties {

    public static final String CONFIG_PREFIX = "registration.breaking-change.check";
    public boolean enabled = false;
    public List<String> whiteListedApps = new ArrayList<>();
}
