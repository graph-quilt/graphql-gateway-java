package com.intuit.graphql.gateway.authorization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.graphql.SchemaManager;
import com.intuit.graphql.gateway.handler.UnprocessableEntityException;
import com.intuit.graphql.gateway.integration.ExpertTestService;
import com.intuit.graphql.gateway.s3.S3RulePackage;
import com.intuit.graphql.gateway.s3.RuleConfig;
import com.intuit.graphql.gateway.s3.RuleConfig.RuleType;
import com.intuit.graphql.gateway.s3.S3Rule;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

public class AuthZValidationServiceTests {

  private AuthZValidationService authZValidationService;


  private String validAuthzQuery =
      "query {\n"
          + "  expert {\n"
          + "    jobTraining {\n"
          + "      learningTeams {\n"
          + "        id\n"
          + "        learningManagementSystem\n"
          + "        name\n"
          + "      }\n"
          + "    }\n"
          + "    jobProfile {\n"
          + "      jobDescription {\n"
          + "        widsProfile\n"
          + "        widsSegment\n"
          + "        widsBusinessUnit\n"
          + "        widsLineOfBusiness\n"
          + "        widsHireType\n"
          + "      }\n"
          + "      skills\n"
          + "    }\n"
          + "    jobPerformance {\n"
          + "      qualityOfServiceSummary {\n"
          + "        latestContactDate\n"
          + "        totalContacts\n"
          + "        agentId\n"
          + "        averageHandleTime\n"
          + "        averageSatisfactionScore\n"
          + "        businessUnits\n"
          + "        channels\n"
          + "        earliestContactDate\n"
          + "        endDate\n"
          + "        startDate\n"
          + "      }\n"
          + "    }\n"
          + "  }\n"
          + "}";

  @Mock
  SchemaManager schemaManager;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void canParseValidAuthZRules() {

    authZValidationService = new AuthZValidationService(schemaManager);
    ExpertTestService expertTestService = new ExpertTestService();
    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .service(expertTestService.getServiceProvider())
        .build().stitchGraph();
    when(schemaManager.getRuntimeGraph()).thenReturn(runtimeGraph);

    final RuleConfig ruleConfig =  RuleConfig.builder()
        .id("test-key")
        .description("test-desc")
        .type(RuleType.OFFLINE).build();

    S3Rule rules = S3Rule.builder()
        .ruleConfig(ruleConfig)
        .queries(Collections.singletonList(validAuthzQuery))
        .build();

    S3RulePackage s3RulePackage = S3RulePackage.builder().authzRulesById(Collections.singletonMap("test-key", rules)).build();

    StepVerifier.create(this.authZValidationService.validate(s3RulePackage))
        .expectNextMatches(stringObjectMap ->
            Objects.nonNull(stringObjectMap) &&
                stringObjectMap.keySet().contains("clients") &&
                stringObjectMap.get("clients").toString().contains("test-key"))
        .verifyComplete();
  }

  @Test
  public void throwsExceptionOnInvalidClientId() {

    authZValidationService = new AuthZValidationService(schemaManager);
    ExpertTestService expertTestService = new ExpertTestService();
    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .service(expertTestService.getServiceProvider())
        .build().stitchGraph();
    when(schemaManager.getRuntimeGraph()).thenReturn(runtimeGraph);

    final RuleConfig ruleConfig =  RuleConfig.builder()
        .description("test-desc")
        .type(RuleType.OFFLINE).build();

    S3Rule rules = S3Rule.builder()
        .ruleConfig(ruleConfig)
        .queries(Collections.singletonList(validAuthzQuery))
        .build();

    S3RulePackage s3RulePackage = S3RulePackage.builder().authzRulesById(Collections.singletonMap("test-key", rules)).build();

    assertThatThrownBy(() -> authZValidationService.validate(s3RulePackage))
        .isInstanceOf(ServerWebInputException.class)
        .hasMessageContaining("Invalid Client Id");
  }

//  @Test
//  public void throwsExceptionOnNoOfflineAuthorizationTypeFound() {
//
//    authZValidationService = new AuthZValidationService(schemaManager);
//    ExpertTestService expertTestService = new ExpertTestService();
//    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
//        .service(expertTestService.getServiceProvider())
//        .build().stitchGraph();
//    when(schemaManager.getRuntimeGraph()).thenReturn(runtimeGraph);
//
//    final RuleConfig ruleConfig =  RuleConfig.builder()
//        .id("test-key")
//        .description("test-desc")
//        .build();
//    S3Rule rules = S3Rule.builder()
//        .ruleConfig(ruleConfig)
//        .queries(Collections.singletonList(validAuthzQuery))
//        .build();
//
//    S3RulePackage s3RulePackage = S3RulePackage.builder().authzRulesById(Collections.singletonMap("test-key", rules)).build();
//
//    assertThatThrownBy(() -> authZValidationService.validate(s3RulePackage))
//        .isInstanceOf(ServerWebInputException.class)
//        .hasMessageContaining("No OFFLINE authorization type found");
//  }

  @Test
  public void throwsExceptionOnErrorInParsingRule() {

    authZValidationService = new AuthZValidationService(schemaManager);
    ExpertTestService expertTestService = new ExpertTestService();
    RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
        .service(expertTestService.getServiceProvider())
        .build().stitchGraph();
    when(schemaManager.getRuntimeGraph()).thenReturn(runtimeGraph);

    final RuleConfig ruleConfig =  RuleConfig.builder()
        .id("test-key")
        .description("test-desc")
        .type(RuleType.OFFLINE).build();
    S3Rule rules = S3Rule.builder()
        .ruleConfig(ruleConfig)
        .queries(Arrays.asList(validAuthzQuery, ""))
        .build();

    S3RulePackage s3RulePackage = S3RulePackage.builder().authzRulesById(Collections.singletonMap("test-key", rules)).build();

    assertThatThrownBy(() -> authZValidationService.validate(s3RulePackage))
        .isInstanceOf(UnprocessableEntityException.class)
        .hasMessageContaining("Failed to parse rule");
  }
}
