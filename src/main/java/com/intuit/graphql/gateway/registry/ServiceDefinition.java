package com.intuit.graphql.gateway.registry;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;

/**
 * A single service definition used for schema registry.
 *
 * Each service needs to have at least a namespace and a endpoint.
 */
@Data
@EqualsAndHashCode()
public class ServiceDefinition implements Serializable {

  @Value("${webclient.timeout}")
  public static long defaultTimeOut = 10000;

  private String appId;
  private String namespace;
  private Type type = Type.GRAPHQL;
  private String endpoint;
  private long timeout = defaultTimeOut;
  private Set<String> forwardHeaders = Collections.emptySet();
  private Set<String> domainTypes = Collections.emptySet();
  private Set<String> clientWhitelist = Collections.emptySet();

  private ServiceDefinition(Builder builder) {
    setAppId(builder.appId);
    setNamespace(builder.namespace);
    setType(builder.type);
    setEndpoint(builder.endpoint);
    setTimeout(builder.timeout);
    setForwardHeaders(builder.forwardHeaders);
    setDomainTypes(builder.domainTypes);
    setClientWhitelist(builder.clientWhitelist);
  }

  public static Builder newBuilder() {
    return new Builder();
  }


  /**
   * Get a map of all loggable fields to use for logging.
   *
   * @return A map of field key to field value for all values that we can/should log
   */
  public Map<String, Object> createEventLoggerFields() {
    Map<String, Object> map = new HashMap<>();
    map.put("appId", this.appId);
    map.put("namespace", this.namespace);
    map.put("type", this.type);
    map.put("endpoint", this.endpoint);
    map.put("timeout", this.timeout);
    return map;
  }

  /**
   * Type of provider service
   */
  public enum Type {
    GRAPHQL, GRAPHQL_SDL, REST, GRPC, V4;

    @JsonCreator
    public static Type fromValue(String value) {
      return valueOf(StringUtils.replaceChars(
          StringUtils.upperCase(value), '-', '_'));
    }

  }

  public static final class Builder {

    private String appId;
    private String namespace;
    private Type type = Type.GRAPHQL;
    private String endpoint;
    private long timeout = defaultTimeOut;
    private Set<String> forwardHeaders = Collections.emptySet();
    private Set<String> domainTypes = Collections.emptySet();
    private Set<String> clientWhitelist = Collections.emptySet();


    private Builder() {
    }

    public Builder appId(String val) {
      appId = val;
      return this;
    }

    public Builder namespace(String val) {
      namespace = val;
      return this;
    }

    public Builder type(Type val) {
      type = val;
      return this;
    }

    public Builder endpoint(String val) {
      endpoint = val;
      return this;
    }

    public Builder timeout(long val) {
      timeout = val;
      return this;
    }

    public Builder forwardHeaders(Set<String> val) {
      forwardHeaders = val;
      return this;
    }

    public Builder domainTypes(Set<String> val) {
      domainTypes = val;
      return this;
    }

    public Builder clientWhitelist(Set<String> val) {
      clientWhitelist = val;
      return this;
    }

    public ServiceDefinition build() {
      return new ServiceDefinition(this);
    }
  }
}
