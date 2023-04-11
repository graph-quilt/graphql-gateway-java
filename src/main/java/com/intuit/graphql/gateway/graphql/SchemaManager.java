package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.events.GraphQLSchemaChangedEvent;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationProvider;
import com.intuit.graphql.gateway.registry.ServiceRegistry;
import com.intuit.graphql.gateway.webclient.TxProvider;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SchemaManager implements ServiceRegistry {

  private final RuntimeGraphBuilder runtimeGraphBuilder;
  private final List<ServiceRegistrationProvider> providers;
  private final ApplicationEventPublisher eventPublisher;
  private final TxProvider txProvider;
  private final ConcurrentMap<String, Set<ServiceRegistration>> serviceDefinitionRegistry;

  private volatile RuntimeGraph runtimeGraph;

  public SchemaManager(final TxProvider txProvider, final RuntimeGraphBuilder runtimeGraphBuilder,
      final List<ServiceRegistrationProvider> providers,
      final ApplicationEventPublisher eventPublisher) {
    this.txProvider = txProvider;
    this.runtimeGraphBuilder = runtimeGraphBuilder;
    this.providers = providers;
    this.eventPublisher = eventPublisher;
    this.serviceDefinitionRegistry = new ConcurrentHashMap<>();
  }

  @PostConstruct
  public void initializeRuntimeGraph() {
    TransactionContext tx = txProvider
        .newTx("REGISTRY-" + UUID.randomUUID().toString());

    Flux<ServiceRegistration> serviceRegistrations = Flux.fromIterable(providers)
        .doOnNext(registry -> registry.registerServiceRegistry(this))
        .flatMap(reg -> reg.getInitialServiceRegistrations()
            .doOnNext(serviceRegistration -> syncServiceRegistration(reg.getRegistryId(), serviceRegistration))
        ).distinct();

    Mono.defer(() ->
        runtimeGraphBuilder.build(serviceRegistrations, true)
            .doOnSuccess(this::updateRuntimeGraph))
        .subscriberContext(context -> context.putNonNull(TransactionContext.class, tx))
        .block();
  }

  public RuntimeGraph getRuntimeGraph() {
    return this.runtimeGraph;
  }

  public void rebuildGraph(TransactionContext tx) {
    Mono.defer(() ->
        runtimeGraphBuilder.build(getCachedRegistrations(), true)
            .doOnSuccess(this::updateRuntimeGraph)
            .doOnSuccess(notUsed -> eventPublisher.publishEvent(GraphQLSchemaChangedEvent.INSTANCE))
    )
        .subscriberContext(context -> context.putNonNull(TransactionContext.class, tx))
        .block();
  }

  /**
   * This method caches the {@link ServiceRegistration} against The ID of its provider {@link
   * ServiceRegistrationProvider}
   *
   * @param registryId          The ID of the {@link ServiceRegistrationProvider} providing the {@link
   *                            ServiceRegistration} to cache
   * @param serviceRegistration The  {@link ServiceRegistration} that is being cached
   */
  private void syncServiceRegistration(String registryId, ServiceRegistration serviceRegistration) {
    Set<ServiceRegistration> serviceRegistrations = serviceDefinitionRegistry.getOrDefault(registryId, new HashSet<>());
    serviceRegistrations.add(serviceRegistration);
    serviceDefinitionRegistry.putIfAbsent(registryId, serviceRegistrations);
  }

  private void updateRuntimeGraph(RuntimeGraph newRuntimeGraph) {
    this.runtimeGraph = newRuntimeGraph;
  }

  @Override
  public Mono<Void> updateRegistry(final String registryId, final Flux<ServiceRegistration> updatedRegistrations) {
    //Remove stale {@link ServiceRegistration}s from cache based on registryId
    serviceDefinitionRegistry.remove(registryId);

    return runtimeGraphBuilder.build(
        Flux.concat(getCachedRegistrations(),
            updatedRegistrations.doOnNext(serviceRegistration ->
                //Store updated {@link ServiceRegistration} in cache
                syncServiceRegistration(registryId, serviceRegistration))
        ).distinct(), true
    ).doOnSuccess(this::updateRuntimeGraph)
        //todo: this is not ideal, there are multiple locations where we publish this event in the same class.
        .doOnSuccess(notUsed -> eventPublisher.publishEvent(GraphQLSchemaChangedEvent.INSTANCE))
        .then();
  }

  /**
   * Returns a  Reactive Sequence of all cached {@link ServiceRegistration}s in {@code serviceRegistryMap}
   *
   * @return A Reactive Sequence of all {@link ServiceRegistration}s cached
   */
  @Override
  public Flux<ServiceRegistration> getCachedRegistrations() {
    return Flux.fromIterable(serviceDefinitionRegistry.values())
        .flatMap(Flux::fromIterable);
  }
}
