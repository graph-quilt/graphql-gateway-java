package com.intuit.graphql.gateway.provider;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.*;
import com.intuit.graphql.orchestrator.ServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class ServiceBuilder {

  private final WebClient webClient;

  public ServiceBuilder(WebClient webClient) {
    this.webClient = webClient;
  }

  /**
   * Build a {@link ServiceProvider} for a single service definition.
   *
   * @param tx Transaction context used for logging
   * @param serviceRegistration Service registration of a single remote GraphQL service
   * @return {@link ServiceProvider} service provider built based on given definitions
   */
  public ServiceProvider buildService(TransactionContext tx, ServiceRegistration serviceRegistration) {

    ServiceDefinition serviceDefinition = serviceRegistration.getServiceDefinition();

    // verify we have a namespace
    if (StringUtils.isEmpty(serviceDefinition.getNamespace())) {
      EventLogger.error(log, tx, "Cannot register service without a namespace.");
      throw new ServiceRegistrationException("Service must have defined namespace.");
    }

    final LogNameValuePair namespace = ImmutableLogNameValuePair
        .of("namespace", serviceRegistration.getServiceDefinition().getNamespace());
    try {
      ServiceProvider serviceProvider = getServiceProvider(tx, serviceRegistration);
      EventLogger.info(log, tx, "Successfully built service provider", namespace);
      return serviceProvider;
    } catch (Exception e) {
      EventLogger.error(log, tx, "Failed to build service provider", e, namespace);
      throw new ServiceRegistrationException("Failed to build service provider" + namespace, e);
    }
  }

  private ServiceProvider getServiceProvider(final TransactionContext tx,
      final ServiceRegistration serviceRegistration) {

    if (serviceRegistration instanceof SdlServiceRegistration) {
      return new SdlServiceProvider((SdlServiceRegistration) serviceRegistration, webClient);
    } else if (serviceRegistration instanceof RestServiceRegistration) {
      return new RestServiceProvider((RestServiceRegistration) serviceRegistration, webClient);
    } else {
      return new IntrospectionServiceProvider(tx, webClient, serviceRegistration);
    }
  }
}
