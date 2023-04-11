package com.intuit.graphql.gateway.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import java.util.Map;
import org.junit.Test;

public class ServiceDefinitionTests {

  @Test
  public void twoDistinctServiceDefinitionsAreNotEqualTest() {
    ServiceDefinition serviceDefinition1 = ServiceDefinition.newBuilder()
        .namespace("test-abc-namespace")
        .endpoint("test-abc-endpoint")
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinition2 = ServiceDefinition.newBuilder()
        .namespace("test-def-namespace")
        .endpoint("test-def-endpoint")
        .type(Type.GRAPHQL).build();

    assertThat(serviceDefinition1.equals(serviceDefinition2)).isFalse();
  }

  @Test
  public void serviceDefinitionsWithDifferentNamespaceAreDifferentTest() {
    ServiceDefinition serviceDefinition1 = ServiceDefinition.newBuilder()
        .namespace("test1")
        .endpoint("same")
        .appId("same")
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinition2 = ServiceDefinition.newBuilder()
        .namespace("test2")
        .endpoint("same")
        .appId("same")
        .type(Type.GRAPHQL).build();

    assertThat(serviceDefinition1.equals(serviceDefinition2)).isFalse();
  }

  @Test
  public void serviceDefinitionsWithSameValuesAreSameTest() {
    ServiceDefinition serviceDefinition1 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("same")
        .appId("same")
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinition2 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("same")
        .appId("same")
        .type(Type.GRAPHQL).build();

    assertThat(serviceDefinition1.equals(serviceDefinition2)).isTrue();
  }

  @Test
  public void serviceDefinitionsWithDifferentEndpointsAreDifferentTest() {
    ServiceDefinition serviceDefinition1 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("different1")
        .appId("same")
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinition2 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("different2")
        .appId("same")
        .type(Type.GRAPHQL).build();

    assertThat(serviceDefinition1.equals(serviceDefinition2)).isFalse();
  }

  @Test
  public void serviceDefinitionsWithDifferentAppIdAreDifferentTest() {
    ServiceDefinition serviceDefinition1 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("same")
        .appId("different1")
        .type(Type.GRAPHQL).build();

    ServiceDefinition serviceDefinition2 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("same")
        .appId("different2")
        .type(Type.GRAPHQL).build();

    assertThat(serviceDefinition1.equals(serviceDefinition2)).isFalse();
  }

  @Test
  public void serviceDefinitionsWithDifferentTypeAreDifferentTest() {
    ServiceDefinition serviceDefinition1 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("same")
        .appId("same")
        .type(Type.REST).build();

    ServiceDefinition serviceDefinition2 = ServiceDefinition.newBuilder()
        .namespace("same")
        .endpoint("same")
        .appId("same")
        .type(Type.GRAPHQL).build();

    assertThat(serviceDefinition1.equals(serviceDefinition2)).isFalse();
  }

  @Test
  public void builderSetsFieldsTest() {
    ServiceDefinition sd = ServiceDefinition.newBuilder()
        .appId("test-appId")
        .namespace("test-namespace")
        .endpoint("test-endpoint")
        .timeout(3000)
        .type(Type.V4)
        .build();

    assertThat(sd.getAppId()).isEqualTo("test-appId");
    assertThat(sd.getNamespace()).isEqualTo("test-namespace");
    assertThat(sd.getEndpoint()).isEqualTo("test-endpoint");
    assertThat(sd.getTimeout()).isEqualTo(3000);
    assertThat(sd.getType()).isEqualTo(Type.V4);
  }

  @Test
  public void getLoggableFieldsHasAllFieldsTest() {
    ServiceDefinition sd = ServiceDefinition.newBuilder()
        .appId("test-appId")
        .endpoint("test-endpoint")
        .namespace("test-namespace")
        .type(Type.REST)
        .build();

    Map<String, Object> loggableFields = sd.createEventLoggerFields();

    assertThat(loggableFields.keySet()).contains("appId", "namespace", "type", "endpoint", "timeout");
  }

  @Test
  public void fromValueCorrectlyParsesToTypeTest() {
    ServiceDefinition.Type type = ServiceDefinition.Type.fromValue("V4");
    assertThat(type).isEqualByComparingTo(ServiceDefinition.Type.V4);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromValueDefaultsToGraphQLOnErrorTest() {
    ServiceDefinition.Type.fromValue("fdsajfdsa");
  }

}
