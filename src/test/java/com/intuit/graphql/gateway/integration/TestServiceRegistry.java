package com.intuit.graphql.gateway.integration;

import static com.intuit.graphql.gateway.TestHelper.createServiceRegistration;

import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistry;
import com.intuit.graphql.gateway.s3.ImmutableEnvironmentSpecification;
import com.intuit.graphql.gateway.s3.ImmutableS3ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.GatewayEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class TestServiceRegistry implements ServiceRegistry {

  @Override
  public Mono<Void> updateRegistry(final String registryId, final Flux<ServiceRegistration> registrations) {
    return Mono.empty();
  }

  @Override
  public Flux<ServiceRegistration> getCachedRegistrations() {
    // create a pre-register test provider
    S3ServiceDefinition.EnvironmentSpecification bookEnvSpec = ImmutableEnvironmentSpecification
        .builder().endpoint("http://localhost:4040/books-introspection/graphql").build();

    Map<GatewayEnvironment, S3ServiceDefinition.EnvironmentSpecification> bookEnvs = new HashMap<>();
    bookEnvs.put(GatewayEnvironment.QA,  bookEnvSpec);

    S3ServiceDefinition bookServiceDefinition = ImmutableS3ServiceDefinition.builder()
        .appId("BookApp.Introspection")
        .namespace("BOOK")
        .environments(bookEnvs)
        .type(Type.GRAPHQL)
        .build();

    return Flux.fromIterable(Arrays.asList(
        createServiceRegistration(bookServiceDefinition.toServiceDefinition("QA", Region.US_WEST_2))
    ));
  }

}
