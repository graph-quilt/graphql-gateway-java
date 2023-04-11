package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.S3ServiceRegistrationProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Provides the API necessary for a Data API instance in EC2 to match the service registry state in S3, and thus, other
 * Data API instances.
 */
public interface S3Poller<T> {

  /**
   * Build a subscribable sequence that is responsible for polling S3 for incremental updates to the service registry.
   * The sequence may update the {@code ServiceRegistry} based on new information fetched from S3.
   *
   * <p>This method has the responsibility of diffing the current state of the local cache vs. what is stored in S3.
   * This method should not perform unnecessary S3 getObject downloads for items that have not changed.
   *
   * @param registry the {@link S3ServiceRegistrationProvider} that the reactive sequence can apply changes to
   * @return a reactive sequence that can be infinitely subscribed to
   */
  Mono<Void> buildPollingSequence(S3Registry<T> registry);

  /**
   * Build a subscribable sequence that is responsible for issuing a "full S3 GET" to update any associated local
   * caches. This method is different from {@link #buildPollingSequence(S3Registry)} in that any call to this method
   * should clear any saved local state and re-hydrate it with the current state in S3.
   *
   * <p>This method should be called on service startup in order to populate the local registry to the current state in
   * S3.
   *
   * @param registry the {@link S3ServiceRegistrationProvider} that the reactive sequence can apply updates to
   * @param tx       the {@link TransactionContext}
   * @return a reactive sequence that can be infinitely subscribed to
   */
  default Mono<Void> fetch(S3Registry<T> registry, TransactionContext tx) {
    return this.buildPollingSequence(registry);
  }

  /**
   * Return a time-capable scheduler that governs the asynchronous execution of reactive sequences provided by {@link
   * #buildPollingSequence(S3Registry)} and {@link #fetch(S3Registry, TransactionContext)}.
   *
   * <p>The specific implementation of this class may dictate which {@code Scheduler} is optimal. For example, you may
   * want to create a single, re-usable scheduler ({@code Schedulers.single()}) that will subscribe to all sequences on
   * the same thread. The default value {@code Schedulers.parallel()} fits most use cases.
   *
   * <p>Overriding this method is also useful for testing, as you can provide a {@code VirtualTimeScheduler} to
   * simulate repetition, subscription delays, and retries over time without using real clock time.
   *
   * @return a time-capable scheduler in which reactive sequences can be run.
   */
  default Scheduler getDefaultScheduler() {
    return Schedulers.parallel();
  }
}
