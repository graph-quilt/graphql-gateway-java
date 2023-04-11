package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import reactor.core.publisher.Mono;

/**
 * @param <T> The objects that are stored internally.
 */
public interface S3Registry<T> {


  /**
   * Returns true if the registry is empty.
   *
   * This method should always return true when the service starts up and this class has not been initialized.
   *
   * @return true if the registry is empty, false otherwise.
   */
  boolean isEmpty();

  /**
   * Returns reactive sequence that can be `subscribed to` to trigger a re-build of any components after a cache
   * update.
   *
   * @return Reactive Sequence to trigger that potentially updates any components that rely on the S3Cache.
   */
  Mono<Void> update();

  /**
   * Updates any related internal cache for a given resource.
   *
   * @param tx       A transactionContext for debugging and tracing
   * @param resource a new or updated resource that needs to be cached
   * @return the resource passed in to enable chaining of method calls
   */
  T cache(TransactionContext tx, T resource);

  /**
   * If the implementing registry stores and caches a single value, return that value.
   *
   * @return the most up-to-date object in the registry
   */
  default T get() {
    throw new UnsupportedOperationException();
  }

  /**
   * If the implementing registry stores multiple values in a collection, retrieve the corresponding object associated
   * with the key. If the implementing registry stores and caches a single value, this method should defer to {@link
   * #get()}.
   *
   * @return the most up-to-date object in the registry, as determined by the provided key
   * @param key Key
   */
  default T get(String key) {
    return get();
  }

  default void delete(TransactionContext tx, String key) {
  }
}
