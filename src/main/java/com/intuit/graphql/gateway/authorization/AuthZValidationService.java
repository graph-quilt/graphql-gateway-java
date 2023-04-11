package com.intuit.graphql.gateway.authorization;

import com.intuit.graphql.gateway.graphql.SchemaManager;
import com.intuit.graphql.gateway.handler.UnprocessableEntityException;
import com.intuit.graphql.gateway.s3.S3RulePackage;
import com.intuit.graphql.gateway.s3.RuleConfig;
import com.intuit.graphql.gateway.s3.RuleConfig.RuleType;
import com.intuit.graphql.gateway.s3.S3Rule;
import com.intuit.graphql.gateway.validator.NotNullValidator;
import com.intuit.graphql.authorization.rules.QueryRuleParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthZValidationService {

  private final SchemaManager schemaManager;
  public static final String ERR_MISSING_SCHEMA = "GraphQL Schema Not Available: %s";
  public static final String ERR_INVALID_CLIENT_ID = "Invalid Client Id: %s";
  public static final String ERR_PARSE_RULE = "Failed to parse rule: %s";

  public AuthZValidationService(SchemaManager schemaManager) {
    this.schemaManager = schemaManager;
  }

  public Mono<Map<String, Object>> validate(S3RulePackage files) {
    Map<String, Object> responseMap = new HashMap<>();

    NotNullValidator.validate(this.schemaManager.getRuntimeGraph())
        .doWhenInvalid(err -> {
          throw new ServerErrorException(String.format(ERR_MISSING_SCHEMA, err.getMessage()), err);
        });

    final Map<RuleConfig, List<String>> rulesByClient = files.getAuthzRulesById().values().stream()
        .filter(s3Rules -> s3Rules.getRuleConfig().getType() == RuleType.OFFLINE)
        .collect(Collectors.toMap(S3Rule::getRuleConfig, S3Rule::getQueries));

    final Set<String> clientIds = rulesByClient.keySet().stream().map(RuleConfig::getId).collect(Collectors.toSet());
    final QueryRuleParser parser = new QueryRuleParser(this.schemaManager.getRuntimeGraph().getExecutableSchema());
    for (Entry<RuleConfig, List<String>> entry : rulesByClient.entrySet()) {
      RuleConfig authzClient = entry.getKey();
      List<String> queries = entry.getValue();
      validateAuthzClient(authzClient, parser, queries);
    }
    responseMap.put("clients", clientIds);
    return Mono.just(responseMap);
  }

  private void validateAuthzClient(RuleConfig ruleConfig, QueryRuleParser parser, List<String> queries) {
    NotNullValidator.validate(ruleConfig.getId())
        .doWhenInvalid(err -> {
          throw new ServerWebInputException(String.format(ERR_INVALID_CLIENT_ID, err.getMessage()));
        });

    for (final String query : queries) {
      try {
        parser.parseRule(query);
      } catch (Exception e) {
        throw new UnprocessableEntityException(String.format(ERR_PARSE_RULE, e.getMessage()));
      }
    }
  }
}
