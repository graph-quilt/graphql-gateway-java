package com.intuit.graphql.gateway.s3;

import lombok.Data;

/**
 * This class represents the versions.yml file found in s3 for AuthZ rules.
 */
@Data
public class S3RuleVersionsFile {

  /**
   * This field holds the absolute path to the authz rules zip file currently used by Data API
   */
  private String latest;
}
