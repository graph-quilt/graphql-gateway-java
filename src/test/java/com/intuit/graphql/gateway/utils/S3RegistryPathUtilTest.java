package com.intuit.graphql.gateway.utils;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.Test;

public class S3RegistryPathUtilTest {

  private static final String TEST_PATH_DEV = "graphql-gateway/dev/registrations/1.0.0/main/graphql/schema.graphql";
  private static final String TEST_PATH_QA = "graphql-gateway/qa/registrations/1.0.0/main/graphql/schema.graphql";

  @Test
  public void toEnvSpecificPath_sameEnv_returnsSamePath() {

    String actual = S3RegistryPathUtil.toEnvSpecificPath(TEST_PATH_DEV, "dev");

    assertThat(actual).isEqualTo(TEST_PATH_DEV);

  }

  @Test
  public void toEnvSpecificPath_NotSameEnv_returnsPathWithUpdatedEnv() {
    String actual = S3RegistryPathUtil.toEnvSpecificPath(TEST_PATH_DEV, "qa");

    assertThat(actual).isEqualTo(TEST_PATH_QA);
  }

}
