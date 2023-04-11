package com.intuit.graphql.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.provider.SdlServiceProvider;
import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.orchestrator.ServiceProvider;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
public class ExpertTestService {

  ServiceDefinition serviceDefinition;
  SdlServiceRegistration registration;
  ServiceProvider serviceProvider;

  private static final WebClient webClient = WebClient.create();
  public static final String SCHEMA_FILE_NAME = "sdls/expert.graphql";

  public ExpertTestService() {
    serviceDefinition = ServiceDefinition.newBuilder()
        .namespace("Expert")
        .endpoint("http://localhost:4040/expert/graphql")
        .timeout(4000)
        .type(Type.GRAPHQL_SDL).build();

    Map<String, String> sdlFiles = null;
    try {
      sdlFiles = ImmutableMap
          .of("expert", Resources.toString(Resources.getResource(SCHEMA_FILE_NAME), Charsets.UTF_8));
    } catch (IOException e) {
      assertThat(true).isFalse();
    }
    registration = SdlServiceRegistration.builder()
        .serviceDefinition(serviceDefinition).graphqlResources(sdlFiles).build();

    serviceProvider = new SdlServiceProvider(registration, webClient);
  }
}
