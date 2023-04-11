package com.intuit.graphql.gateway.registry;

import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.common.InvalidGatewayEnvironmentException;
import com.intuit.graphql.gateway.handler.UnprocessableEntityException;
import com.intuit.graphql.gateway.s3.FileEntry;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.GatewayEnvironment;
import com.intuit.graphql.orchestrator.stitching.StitchingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.server.ServerWebInputException;

public class ServiceRegistrationUtil {

  private ServiceRegistrationUtil() {
  }

  /**
   * creates ServiceRegistration from the given byte array in zip format and environmen t
   *
   * @param fileEntries registration files
   * @param env environment.  Should conform to {@link GatewayEnvironment}
   * @param region The aws region
   * @return new instances of {@link ServiceRegistration}
   */
  public static ServiceRegistration createServiceRegistrationFromZip(List<FileEntry> fileEntries, String env, Region region) {
    S3ServiceDefinition s3ServiceDefinition = null;
    Map<String, String> flowResources = new HashMap<>();
    Map<String, String> graphqlsResources = new HashMap<>();

    for (FileEntry fileEntry : fileEntries) {
      if (Predicates.isMainFolder.test(fileEntry.filename())
          && Predicates.isMainConfigJson.test(fileEntry.filename())) {
        try {
          s3ServiceDefinition = Mapper.mapper().readValue(new String(fileEntry.contentInBytes()), S3ServiceDefinition.class);
          s3ServiceDefinition.toServiceDefinition(env, region);
          if (!s3ServiceDefinition.hasDefinedEnvironment(env, region)) {
            throw new ConfigJsonException(
                String.format("Cannot get endpoint for appdId = %s, env=%s", s3ServiceDefinition.appId(), env));
          }
        } catch (Exception e) {
          throw new ConfigJsonException("Error parsing main/config.json:"+e.getMessage());
        }
      } else if (Predicates.isSDLFile.test(fileEntry.filename())) {
        graphqlsResources.put(fileEntry.filename(), new String(fileEntry.contentInBytes()));
      } else if (Predicates.isFlowFile.test(fileEntry.filename())) {
        flowResources.put(fileEntry.filename(), new String(fileEntry.contentInBytes()));
      }// end if-else
    } // end for

    if (Objects.nonNull(s3ServiceDefinition)) {
      return createServiceRegistration(s3ServiceDefinition.toServiceDefinition(env, region), flowResources, graphqlsResources);
    } else {
      throw new ConfigJsonException("Missing main/config.json");
    }
  }

  /**
   * creates ServiceRegistration which can be of type REST, GRAPHQL_SDL or base ServiceRegistration
   *
   * @param serviceDefinition Service Definition
   * @param flowResources Flow file
   * @param graphqlsResources GraphQLResources
   * @return new instance of {@link ServiceRegistration}
   */
  public static ServiceRegistration createServiceRegistration(ServiceDefinition serviceDefinition,
      Map<String, String> flowResources, Map<String, String> graphqlsResources) {

    switch (serviceDefinition.getType()) {
      case REST:
        return RestServiceRegistration.builder()
            .flowResources(flowResources)
            .graphqlResources(graphqlsResources)
            .serviceDefinition(serviceDefinition)
            .build();
      case GRAPHQL_SDL:
        return SdlServiceRegistration.builder()
            .graphqlResources(graphqlsResources)
            .serviceDefinition(serviceDefinition)
            .build();
      case GRAPHQL:
      default:
        return ServiceRegistration.baseBuilder()
            .serviceDefinition(serviceDefinition)
            .build();
    }
  }

  /**
   * get the {@link ServiceDefinition.Type} for the given {@link ServiceRegistration}
   *
   * @param serviceRegistration {@link ServiceRegistration} to be processed
   * @return {@link ServiceDefinition.Type}
   */
  public static ServiceDefinition.Type getServiceDefinitionType(ServiceRegistration serviceRegistration) {
    Objects.requireNonNull(serviceRegistration,
        "serviceRegistration required to get ServiceDefinition.Type");
    Objects.requireNonNull(serviceRegistration.getServiceDefinition(),
        "serviceRegistration.serviceDefinition required to get ServiceDefinition.Type");
    return serviceRegistration.getServiceDefinition().getType();
  }

  /**
   * checks if the given {@link ServiceRegistration} refers to the same service using its {@link ServiceDefinition}
   * appId
   *
   * @param svcReg1 first {@link ServiceRegistration}
   * @param svcReg2 second {@link ServiceRegistration}
   * @return true if has the same service.  Otherwise returns false.
   */
  public static boolean isSameService(ServiceRegistration svcReg1, ServiceRegistration svcReg2) {
    return StringUtils.equals(svcReg1.getServiceDefinition().getAppId(), svcReg2.getServiceDefinition().getAppId());
  }

  /**
   * Checks if the given {@link ServiceRegistration} has the same schema or not.
   *
   * @param svcReg1 first {@link ServiceRegistration}
   * @param svcReg2 second {@link ServiceRegistration}
   * @return true if has the same schema.  Otherwise returns false.
   */
  public static boolean isNotSameSchema(ServiceRegistration svcReg1, ServiceRegistration svcReg2) {
    return !Objects.equals(svcReg1, svcReg2);
  }

  /**
   * Returns a set containing paths of all graphql and flow resources (if any) from a service registration
   *
   * @param svcReg the service registration
   * @return return a set of resource paths
   */
  public static Set<String> getResourcePaths(ServiceRegistration svcReg) {
    Set<String> resourcePaths = new HashSet<>();

    ServiceDefinition.Type type = ServiceRegistrationUtil.getServiceDefinitionType(svcReg);
    Objects.requireNonNull(type);

    switch (type) {
      case GRAPHQL_SDL:
        resourcePaths.addAll(((SdlServiceRegistration) svcReg).getGraphqlResources().keySet());
        break;
      case GRAPHQL:
      default:
    }
    return resourcePaths;
  }

  public static void throwSpringException(Throwable err) {
    //ToDo: Revisit exception hierarchy in stitching
    if (err instanceof ServiceRegistrationException) {
      //SchemaTransformation exception is also Stitching exception.
      Throwable cause = err.getCause();
      String message = cause instanceof StitchingException ?
          cause.getMessage() : err.getMessage();
      throw new UnprocessableEntityException(message); //422 response
    }
    if (err instanceof InvalidGatewayEnvironmentException) {
      throw new ServerWebInputException(err.getMessage()); //400 response
    }
  }
}
