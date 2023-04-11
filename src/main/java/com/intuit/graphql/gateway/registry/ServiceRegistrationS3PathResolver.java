package com.intuit.graphql.gateway.registry;

import com.intuit.graphql.gateway.s3.FilePathResolver;
import java.util.HashMap;
import java.util.Map;

public class ServiceRegistrationS3PathResolver {

  private FilePathResolver s3PathResolver;

  public ServiceRegistrationS3PathResolver(FilePathResolver s3PathResolver) {
    this.s3PathResolver = s3PathResolver;
  }

  /**
   * Converts resource paths to paths in S3
   *
   * @param serviceRegistration input ServiceRegistration
   * @return an updated ServiceRegistration
   */
  public ServiceRegistration resolve(ServiceRegistration serviceRegistration) {
    if (serviceRegistration instanceof SdlServiceRegistration) {
      return resolve((SdlServiceRegistration) serviceRegistration);
    } else if (serviceRegistration instanceof RestServiceRegistration) {
      return resolve((RestServiceRegistration) serviceRegistration);
    } else {
      return serviceRegistration;
    }
  }

  private ServiceRegistration resolve(SdlServiceRegistration sdlServiceRegistration) {
    String appId = sdlServiceRegistration.getServiceDefinition().getAppId();

    Map<String, String> newGraphQLResources = resolveResourcePaths(sdlServiceRegistration.getGraphqlResources(), appId);

    return SdlServiceRegistration
        .builder()
        .serviceDefinition(sdlServiceRegistration.getServiceDefinition())
        .graphqlResources(newGraphQLResources)
        .build();
  }

  private ServiceRegistration resolve(RestServiceRegistration restServiceRegistration) {
    String appId = restServiceRegistration.getServiceDefinition().getAppId();

    Map<String, String> newGraphQLResources = resolveResourcePaths(restServiceRegistration.getGraphqlResources(), appId);
    Map<String, String> newFlowResources = resolveResourcePaths(restServiceRegistration.getFlowResources(), appId);

    return RestServiceRegistration.builder()
        .flowResources(newFlowResources)
        .graphqlResources(newGraphQLResources)
        .serviceDefinition(restServiceRegistration.getServiceDefinition())
        .build();

  }

  private Map<String, String> resolveResourcePaths(Map<String, String> inputResourceMap, String appId) {
    Map<String, String> outputResourceMap = new HashMap<>();

    inputResourceMap.keySet()
        .forEach(oldKey -> {
          String newKey = s3PathResolver.getResolvedPath(oldKey, appId);
          outputResourceMap.put(newKey, inputResourceMap.get(oldKey));
        });

    return outputResourceMap;
  }

}
