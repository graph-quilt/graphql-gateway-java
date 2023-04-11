package com.intuit.graphql.gateway.s3;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationUtil;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * This class is used to store all resources of a {@link ServiceRegistration} read from S3 bucket.
 */
@Getter
public class S3RegistrationCache {

  private ServiceDefinition serviceDefinition;
  /**
   * Provides constant-time lookup for Flow resources based on their s3 key.
   */
  @VisibleForTesting
  private Map<String, String> objectKeyFlowResourceMap;

  /**
   * Provides constant-time lookup for Graphql resources based on their s3 key.
   */
  @VisibleForTesting
  private Map<String, String> objectKeyGraphqlResourceMap;


  public S3RegistrationCache() {
    this.objectKeyFlowResourceMap = new ConcurrentHashMap<>();
    this.objectKeyGraphqlResourceMap = new ConcurrentHashMap<>();
  }

  public S3RegistrationCache setServiceDefinition(ServiceDefinition serviceDefinition) {
    this.serviceDefinition = serviceDefinition;
    return this;
  }

  public void addFlowResource(String key, String flowResource) {
    objectKeyFlowResourceMap.put(key, flowResource);
  }

  public void addGraphqlResource(String key, String schemaDefinition) {
    objectKeyGraphqlResourceMap.put(key, schemaDefinition);
  }

  public void removeResource(String key) {
    if (Objects.nonNull(key)) {
      this.objectKeyFlowResourceMap.remove(key);
      this.objectKeyGraphqlResourceMap.remove(key);
    }
  }

  /**
   * Creates and returns respective instance of {@link ServiceRegistration ServiceRegistration} from cache based on the
   * {@link Type type} of registration.
   *
   * @return ServiceRegistration instance based on type from the cache
   */
  public ServiceRegistration toServiceRegistration() {

    return ServiceRegistrationUtil
        .createServiceRegistration(serviceDefinition, ImmutableMap.copyOf(getObjectKeyFlowResourceMap()),
            ImmutableMap.copyOf(getObjectKeyGraphqlResourceMap()));

  }
}