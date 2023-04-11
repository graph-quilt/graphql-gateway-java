package com.intuit.graphql.gateway.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(builderMethodName = "baseBuilder")
@EqualsAndHashCode
public class ServiceRegistration {

  private ServiceDefinition serviceDefinition;

}
