package com.intuit.graphql.gateway.registry;

import reactor.core.publisher.Flux;

/**
 * Any implementing class should handle all {@link ServiceRegistration} updates for its purpose. Examples of various
 * implementing "purposes" are: Config as a Service, S3, Local, etc..
 */
public interface ServiceRegistrationProvider {

  /**
   * Get the ID of this {@link ServiceRegistrationProvider}. This is used when calling the {@link
   * ServiceRegistry#updateRegistry(String, Flux)} providers in order to identify itself with the {@link
   * ServiceRegistry}.
   *
   * @return ID for this {@link ServiceRegistrationProvider}
   */
  default String getRegistryId() {
    return this.getClass().getName();
  }

  /**
   * Gets the initial {@link ServiceRegistration ServiceRegistrations} for this {@link ServiceRegistrationProvider}. It
   * should be assumed that the values returned by this function are used by at least some clients until the {@link
   * ServiceRegistrationProvider} triggers the {@link ServiceRegistry#updateRegistry(String, Flux)} method. This
   * function may be blocking, but application start-up will be affected. Though, this is usually preferable over
   * serving clients with incorrect {@link ServiceRegistration ServiceRegistrations}. Any subsequent updates to
   * providers should be triggered via the {@link ServiceRegistry#updateRegistry(String, Flux)} method.
   *
   * @return initial {@link ServiceRegistration ServiceRegistrations}
   */
  Flux<ServiceRegistration> getInitialServiceRegistrations();

  /**
   * Used by the {@link ServiceRegistry} to register itself with each {@link ServiceRegistrationProvider}. When a {@link
   * ServiceRegistrationProvider} has new {@link ServiceRegistration ServiceRegistrations}, it calls the {@link
   * ServiceRegistry#updateRegistry(String, Flux)} method to update providers.
   *
   * @param serviceRegistry Class that listens to {@link ServiceRegistration} updates from the {@link
   * ServiceRegistrationProvider}
   */
  void registerServiceRegistry(ServiceRegistry serviceRegistry);

}
