package com.intuit.graphql.gateway.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.common.InvalidGatewayEnvironmentException;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.GatewayEnvironment;
import java.io.IOException;
import org.junit.Test;

public class S3ServiceDefinitionTest {

  @Test
  public void fromValueCorrectlyParsesToGatewayEnvironmentTest() {
    GatewayEnvironment gatewayEnvironment = GatewayEnvironment.fromValue("prod-stg");
    assertThat(gatewayEnvironment).isEqualByComparingTo(gatewayEnvironment.PROD_STG);

    gatewayEnvironment = GatewayEnvironment.fromValue("dev");
    assertThat(gatewayEnvironment).isEqualByComparingTo(gatewayEnvironment.DEV);

    gatewayEnvironment = GatewayEnvironment.fromValue("qa");
    assertThat(gatewayEnvironment).isEqualByComparingTo(gatewayEnvironment.QA);

    gatewayEnvironment = GatewayEnvironment.fromValue("e2e");
    assertThat(gatewayEnvironment).isEqualByComparingTo(gatewayEnvironment.E2E);

    gatewayEnvironment = GatewayEnvironment.fromValue("perf");
    assertThat(gatewayEnvironment).isEqualByComparingTo(gatewayEnvironment.PERF);

    gatewayEnvironment = GatewayEnvironment.fromValue("prod");
    assertThat(gatewayEnvironment).isEqualByComparingTo(gatewayEnvironment.PROD);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromValueDefaultsToGraphQLOnErrorTest() {
    GatewayEnvironment.fromValue("fdsajfdsa");
  }

  @Test(expected = IllegalArgumentException.class)
  public void toServiceDefinitionThrowsExceptionOnFailureTest() {
    S3ServiceDefinition s3ServiceDefinition = ImmutableS3ServiceDefinition.builder().appId("appId")
        .namespace("namespace").build();
    assertNull(s3ServiceDefinition.toServiceDefinition("env", Region.US_WEST_2));
  }

  @Test
  public void testRegionSecificConfig() throws IOException {
    String contents = Resources.toString(Resources.getResource(
        "provider-configs/config_v4os_with_regions.json"), Charsets.UTF_8);
    S3ServiceDefinition s3ServiceDefinition = Mapper.mapper().readValue(contents, S3ServiceDefinition.class);

    ServiceDefinition configDevWest = s3ServiceDefinition.toServiceDefinition("dev", Region.US_WEST_2);
    assertThat(configDevWest.getEndpoint()).isEqualTo("https://test-app-dev-west.api.intuit.com/graphql");
    assertThat(configDevWest.getTimeout()).isEqualTo(5001);

    ServiceDefinition configQaEast = s3ServiceDefinition.toServiceDefinition("qa", Region.US_EAST_2);
    assertThat(configQaEast.getEndpoint()).isEqualTo("https://test-app-cdev-east.api.intuit.com/graphql");
    assertThat(configQaEast.getTimeout()).isEqualTo(5002);

    ServiceDefinition configE2eWest = s3ServiceDefinition.toServiceDefinition("e2e", Region.US_WEST_2);
    assertThat(configE2eWest.getEndpoint()).isEqualTo("https://test-app-e2e-west.api.intuit.com/graphql");
//    assertThat(configE2eWest.getTimeout()).isEqualTo(5003); TODO: default timeout should not take priority

    ServiceDefinition configPerfWest = s3ServiceDefinition.toServiceDefinition("perf", Region.US_WEST_2);
    assertThat(configPerfWest.getEndpoint()).isEqualTo("http://test-app-prf.api.intuit.com/graphql");
    assertThat(configPerfWest.getTimeout()).isEqualTo(4000);

    ServiceDefinition configPerfEast = s3ServiceDefinition.toServiceDefinition("perf", Region.US_EAST_2);
    assertThat(configPerfEast.getEndpoint()).isEqualTo("http://test-app-prf-east.api.intuit.com/graphql");
    assertThat(configPerfEast.getTimeout()).isEqualTo(4000);

    ServiceDefinition configProdWest = s3ServiceDefinition.toServiceDefinition("prod", Region.US_WEST_2);
    assertThat(configProdWest.getEndpoint()).isEqualTo("https://test-app.api.intuit.com/graphql");
    assertThat(configProdWest.getTimeout()).isEqualTo(10000);
  }

  @Test
  public void testEnvironmentSecificClientWhitelist() throws IOException {
    String contents = Resources.toString(Resources.getResource(
        "provider-configs/config_v4os_with_clientwhitelist_per_env.json"), Charsets.UTF_8);
    S3ServiceDefinition s3ServiceDefinition = Mapper.mapper().readValue(contents, S3ServiceDefinition.class);

    ServiceDefinition configDevWest = s3ServiceDefinition.toServiceDefinition("dev", Region.US_WEST_2);
    assertThat(configDevWest.getClientWhitelist()).containsExactlyInAnyOrder("intuit.android.test-client");

    ServiceDefinition configQaEast = s3ServiceDefinition.toServiceDefinition("qa", Region.US_EAST_2);
    assertThat(configQaEast.getClientWhitelist()).containsExactlyInAnyOrder("intuit.services.test-service", "intuit.ios.test-client");

    ServiceDefinition configProdWest = s3ServiceDefinition.toServiceDefinition("prod", Region.US_WEST_2);
    assertThat(configProdWest.getClientWhitelist()).containsExactlyInAnyOrder("intuit.browser.test-client");
  }

  @Test(expected = InvalidGatewayEnvironmentException.class)
  public void testMissingEnvironmentinConfigjson() throws IOException {
    String contents = Resources.toString(Resources.getResource(
        "provider-configs/config_v4os_with_regions.json"), Charsets.UTF_8);
    S3ServiceDefinition s3ServiceDefinition = Mapper.mapper().readValue(contents, S3ServiceDefinition.class);
    s3ServiceDefinition.toServiceDefinition("prod-stg", Region.US_WEST_2);
  }
}
