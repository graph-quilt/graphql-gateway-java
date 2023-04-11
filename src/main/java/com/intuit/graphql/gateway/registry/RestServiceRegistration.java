package com.intuit.graphql.gateway.registry;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class RestServiceRegistration extends ServiceRegistration {

  private Map<String, String> flowResources;
  private Map<String, String> graphqlResources;

  @Builder
  public RestServiceRegistration(ServiceDefinition serviceDefinition, Map<String, String> flowResources,
      Map<String, String> graphqlResources) {
    super(serviceDefinition);
    this.flowResources = flowResources;
    this.graphqlResources = graphqlResources;
  }

}
