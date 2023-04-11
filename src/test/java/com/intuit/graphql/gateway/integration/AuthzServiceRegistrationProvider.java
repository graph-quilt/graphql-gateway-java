package com.intuit.graphql.gateway.integration;

import com.google.common.io.Resources;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationProvider;
import com.intuit.graphql.gateway.registry.ServiceRegistry;
import com.intuit.graphql.gateway.s3.ImmutableEnvironmentSpecification;
import com.intuit.graphql.gateway.s3.ImmutableS3ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.GatewayEnvironment;
import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.EnvironmentSpecification;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

@Configuration
@Profile("test-authz")
public class AuthzServiceRegistrationProvider implements ServiceRegistrationProvider {

  private String getSdlFile(String filename) {
    try {
      return Resources.toString(Resources.getResource("integration/schemas/" + filename), Charset.defaultCharset());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Flux<ServiceRegistration> getInitialServiceRegistrations() {

    EnvironmentSpecification bookEnvSpec = ImmutableEnvironmentSpecification
        .builder().endpoint("http://localhost:4040/authz/graphql").build();

    Map<GatewayEnvironment, EnvironmentSpecification> bookEnvs = new HashMap<>();
    bookEnvs.put(GatewayEnvironment.QA, bookEnvSpec);

    S3ServiceDefinition bookServiceDefinition = ImmutableS3ServiceDefinition.builder()
        .appId("BookApp.Introspection")
        .namespace("Authz")
        .environments(bookEnvs)
        .type(Type.GRAPHQL_SDL)
        .build();

    Map<String, String> baseTypeProviderSdlFiles = new HashMap<>();
    baseTypeProviderSdlFiles.put("testAuthzSchema", getSdlFile("testAuthzSchema.graphql"));

    return Flux.fromIterable(Arrays.asList(
        TestHelper.createSdlServiceRegistration(bookServiceDefinition.toServiceDefinition(GatewayEnvironment.QA.toString(), Region.US_WEST_2),
            baseTypeProviderSdlFiles)
    ));
  }

  @Override
  public void registerServiceRegistry(ServiceRegistry serviceRegistry) {
    //do nothing
  }
}
