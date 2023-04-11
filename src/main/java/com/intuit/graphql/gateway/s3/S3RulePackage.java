package com.intuit.graphql.gateway.s3;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class S3RulePackage {

  /**
   * AuthZ Rules separated by directory parent name
   *
   * e.g. zip-folder-root/appid/config.yml
   *
   * The key for this map would be "appid"
   */
  private final Map<String, S3Rule> authzRulesById;

}
