package com.intuit.graphql.gateway.registry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceRegistry {

  /**
   * Returns reactive sequence that can be Used by {@link ServiceRegistrationProvider} to trigger a new build of
   * orchestrator with the latest {@link ServiceRegistration}s.
   *
   * @param registryId The ID of the {@link ServiceRegistrationProvider} that is updating its {@link ServiceRegistration
   * ServiceRegistrations}
   * @param registrations The latest {@link ServiceRegistration ServiceRegistrations} for a given {@link
   * ServiceRegistrationProvider}
   * @return Reactive sequence that can be subscribed to build graphql orchestrator
   */
  Mono<Void> updateRegistry(String registryId, Flux<ServiceRegistration> registrations);

  Flux<ServiceRegistration> getCachedRegistrations();

}
