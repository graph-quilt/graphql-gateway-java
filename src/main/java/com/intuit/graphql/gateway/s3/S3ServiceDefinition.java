package com.intuit.graphql.gateway.s3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.intuit.graphql.gateway.common.InvalidGatewayEnvironmentException;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.springframework.beans.factory.annotation.Value;

/**
 * Used for deserialization of an S3 registration object. Usually this object is not used directly, but is used to get
 * the derived {@link ServiceDefinition} based on the desired environment.
 */
@JsonSerialize(as = ImmutableS3ServiceDefinition.class)
@JsonDeserialize(as = ImmutableS3ServiceDefinition.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Style(jdkOnly = true)
@Immutable
@Slf4j
public abstract class S3ServiceDefinition implements Serializable {

  @Value("${webclient.timeout}")
  public static long defaultTimeOut = 10000;

  public abstract Map<GatewayEnvironment, EnvironmentSpecification> environments();

  public abstract String appId();

  public abstract String namespace();

  @JsonProperty("domain-types")
  public abstract Set<String> domainTypes();

  @JsonProperty("forward-headers")
  public abstract Set<String> forwardHeaders();

  @JsonProperty("client-whitelist")
  public abstract Set<String> clientWhitelist();

  @Default
  public Type type() {
    return Type.GRAPHQL;
  }

  /**
   * Get {@link EnvironmentSpecification} based on region specific environment, if configuration is not found use the
   * base region agnostic environment to fetch the environment spec.
   *
   * @param env The environment spec returned may differ based on which environment we are in. Refer to {@link
   * GatewayEnvironment} for valid environments.
   * @param region The aws region
   * @return The {@link EnvironmentSpecification} object derived from information in this {@link S3ServiceDefinition}
   * object.
   */
  public EnvironmentSpecification getEnvSpec(String env, Region region) {
    GatewayEnvironment baseEnv = GatewayEnvironment.fromValue(env);
    EnvironmentSpecification baseSpec = environments().get(baseEnv);

    //Set default timeout if not defined in base spec
    if(Objects.nonNull(baseSpec) && Objects.isNull(baseSpec.timeout())) {
      baseSpec = ImmutableEnvironmentSpecification.builder()
          .from(environments().get(baseEnv))
          .timeout(defaultTimeOut)
          .build();
    }
    // Region specific checks
    EnvironmentSpecification regionalSpec = null;
    if (Objects.nonNull(region) && Objects.nonNull(baseSpec)) {
      regionalSpec = region == Region.US_WEST_2 ? baseSpec.uswest2() : baseSpec.useast2();
    }
    if (Objects.isNull(regionalSpec)) {
      return baseSpec;
    }
    return ImmutableEnvironmentSpecification.builder()
        .from(baseSpec)
        .endpoint(ObjectUtils.firstNonNull(regionalSpec.endpoint(), baseSpec.endpoint()))
        .timeout(ObjectUtils.firstNonNull(regionalSpec.timeout(), baseSpec.timeout(), defaultTimeOut))
        .build();
  }

  /**
   * Get the {@link ServiceDefinition} based on information in this {@link S3ServiceDefinition} object. The endpoint may
   * change based on desired environment.
   *
   * @param env The endpoint used may differ based on which environment we are in. Refer to {@link GatewayEnvironment}
   * for valid environments.
   * @param region The aws region
   * @return The {@link ServiceDefinition} object derived from information in this {@link S3ServiceDefinition} object.
   */
  @Derived
  public ServiceDefinition toServiceDefinition(String env, Region region) {

    EnvironmentSpecification envSpec = getEnvSpec(env, region);
    if (Objects.isNull(envSpec)) {
      throw new InvalidGatewayEnvironmentException(String
          .format("Failed to find environment specification [%s,%s] for namespace:%s.", env, region, namespace()));
    }
    Set<String> clientWhiteList = CollectionUtils.isNotEmpty(envSpec.clientWhitelist()) ? envSpec.clientWhitelist() : clientWhitelist();
    return ServiceDefinition.newBuilder()
        .appId(appId())
        .namespace(namespace())
        .type(type())
        .forwardHeaders(forwardHeaders())
        .timeout(envSpec.timeout())
        .endpoint(envSpec.endpoint())
        .domainTypes(domainTypes())
        .clientWhitelist(clientWhiteList)
        .build();
  }

  public boolean hasDefinedEnvironment(String env, Region region) {
    return Objects.nonNull(getEnvSpec(env, region));
  }

  /**
   * Valid environments to use when getting the service endpoint.
   */
  public enum GatewayEnvironment {
    DEV, QA, PERF, E2E, PROD_STG, PROD;

    private static final String ERR_INVALID_ENV = "Invalid environment: %s";

    @JsonCreator
    public static GatewayEnvironment fromValue(String value) {

      return valueOf(StringUtils.replaceChars(
          StringUtils.upperCase(value), '-', '_'));
    }

    @JsonCreator
    public static GatewayEnvironment fromNullableValue(String value) {
      if (Objects.isNull(value)) {
        return null;
      }
      try {
        return fromValue(value);
      } catch (IllegalArgumentException e) {
        throw new InvalidGatewayEnvironmentException(String.format(ERR_INVALID_ENV, value));
      }
    }

    public String toKebabCase() {
      return StringUtils.replaceChars(toString().toLowerCase(), '_', '-');
    }

  }

  /**
   * Holding class for a service's endpoint.
   */
  @Immutable
  @JsonSerialize(as = ImmutableEnvironmentSpecification.class)
  @JsonDeserialize(as = ImmutableEnvironmentSpecification.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public abstract static class EnvironmentSpecification {

    @JsonProperty("client-whitelist")
    public abstract Set<String> clientWhitelist();

    abstract String endpoint();

    @Nullable
    public abstract Long timeout();

    @Nullable
    public abstract EnvironmentSpecification uswest2();

    @Nullable
    public abstract EnvironmentSpecification useast2();
  }
}


