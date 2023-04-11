package com.intuit.graphql.gateway.s3;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class S3Rule {

  private final RuleConfig ruleConfig;
  private final List<String> queries;
  private final String rulebase;
}
