package com.intuit.graphql.gateway.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Authz rule configuration class that is deserialized from a config.yml of a authz registration.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RuleConfig {

  private String id;
  private String description;
  private RuleType type;

  public enum RuleType {
    OFFLINE, ONLINE
  }
}

