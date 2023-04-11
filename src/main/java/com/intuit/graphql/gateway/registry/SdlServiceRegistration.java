package com.intuit.graphql.gateway.registry;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class SdlServiceRegistration extends ServiceRegistration {

  private Map<String, String> graphqlResources;

  @Builder
  public SdlServiceRegistration(ServiceDefinition serviceDefinition, Map<String, String> graphqlResources) {
    super(serviceDefinition);
    this.graphqlResources = graphqlResources;
  }
}
