package com.intuit.graphql.gateway.s3;

import com.google.common.annotations.VisibleForTesting;
import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.s3.RegistrationPoller.RegistrationResource;
import com.intuit.graphql.gateway.webclient.TxProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * The Registration Poller maintains up-to-date service registrations across all Data API instances.
 */
@Component
@Slf4j
public class RegistrationPoller implements S3Poller<RegistrationResource> {

  private final SchemaRegistrationS3Client s3Client;
  @VisibleForTesting
  Map<String, S3Object> registrationCache;
  private final TxProvider txProvider;
  private final S3Configuration s3Configuration;

  public RegistrationPoller(final SchemaRegistrationS3Client s3Client,
      final TxProvider txProvider, final S3Configuration s3Configuration) {
    this.s3Client = s3Client;
    this.txProvider = txProvider;
    this.s3Configuration = s3Configuration;
    this.registrationCache = new ConcurrentHashMap<>();
  }

  /**
   * Get all s3 objects that require our attention. In other words, any s3 objects that are not just the same as what we
   * had last time we checked.
   *
   * @return A Flux of all the s3 objects that require our attention
   */
  private Flux<S3Object> validRegistrations(final TransactionContext tx,
      final S3Registry<RegistrationResource> registry) {
    return s3Client.listRegistrations()
        .buffer()
        .doOnNext(s3Objects -> this.deleteInvalidRegistrations(tx, s3Objects, registry))
        .flatMap(Flux::fromIterable)
        .filter(this::shouldUpdateRegistration);
  }

  /**
   * Log a (potentially null) s3 object.
   *
   * @param tx transaction context used for logging
   * @param t  the thrown exception/error to include in the log
   * @param o  the offending object to log (unless it is null!)
   */
  private void logOffendingObject(TransactionContext tx, Throwable t, Object o) {
    if (o == null) {
      EventLogger.error(log, tx, "Failed to sync registration", t);
    } else {
      EventLogger.error(log, tx, "Failed to sync registration", t, ImmutableLogNameValuePair.of("object", o.toString()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> buildPollingSequence(final S3Registry<RegistrationResource> registry) {
    TransactionContext tx = txProvider.newTx("REGISTRY-" + UUID.randomUUID().toString());
    return Flux.defer(()-> this.validRegistrations(tx, registry))
        .flatMap(s3Object -> download(tx, s3Object))
        .map(registrationResource -> registry.cache(tx, registrationResource))
        .onErrorContinue((t, o) -> logOffendingObject(tx, t, o))
        .count()
        .flatMap(aLong -> aLong > 0 ? registry.update() : Mono.empty())
        .then()
        .doOnSubscribe(subscription -> EventLogger.info(log, tx, "Syncing registrations"))
        .doOnError(err -> EventLogger.error(log, tx, "Error syncing registrations", err))
        .subscriberContext(context -> context.put(TransactionContext.class, tx));

    //TODO: Edge case when a file is deleted, no new files added, schema should be rebuilt.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> fetch(final S3Registry<RegistrationResource> registry, TransactionContext tx) {
    return Flux.defer(s3Client::listRegistrations)
        .flatMap(s3Object -> download(tx, s3Object))
        .map(registrationResource -> registry.cache(tx, registrationResource))
        .onErrorContinue((t, o) -> logOffendingObject(tx, t, o))
        .then()
        .doOnSubscribe(subscription -> EventLogger.info(log, tx, "Fetching new registrations"))
        .doOnError(err -> EventLogger.error(log, tx, "Error fetching new registrations", err))
        .subscriberContext(c -> c.put(TransactionContext.class, tx));
  }

  /**
   * Override the default scheduler to only use 1 thread initially for single polling sequence (to fetch service
   * definitions). We can change this to use multiple threads if more sequences are added.
   */
  @Override
  public Scheduler getDefaultScheduler() {
    return Schedulers.single();
  }

  /**
   * Fetch the new or updated service registration from S3 and update the local service registration cache on success.
   *
   * @param tx       transaction context used for logging
   * @param s3Object registration S3Object found in S3 updated
   * @return a reactive sequence that can be subscribed to
   */
  private Mono<RegistrationResource> download(final TransactionContext tx, final S3Object s3Object) {
    EventLogger.info(log, tx, "Found new resource", ImmutableLogNameValuePair.of("key", s3Object.key()));
    return s3Client.downloadRegistrationResource(s3Object)
        .doOnSuccess(x -> registrationCache.put(s3Object.key(), s3Object))
        .doOnError(e -> EventLogger
            .error(log, tx, "Failed to fetch new resource.", e, ImmutableLogNameValuePair.of("key", s3Object.key())));
  }

  /**
   * Helper function to determine if we should update our local service registrations.
   *
   * @param s3Object The S3 object to check if we need to update our local registrations based on
   * @return True if we should update our local registrations; False otherwise
   */
  private boolean shouldUpdateRegistration(final S3Object s3Object) {

    // is the s3 object new? (we dont have it stored in our cache)
    boolean isS3ObjectNew = !registrationCache.containsKey(s3Object.key());

    // do we have the s3 object but it has been modified since we last checked?
    boolean isS3ObjectModified = registrationCache.containsKey(s3Object.key()) &&
        !registrationCache.get(s3Object.key()).lastModified().equals(s3Object.lastModified());

    // wait for delay period before syncing
    boolean isObjectSyncReady = true;
    if (!s3Configuration.getPolling().getSyncDelay().isNegative()) {
      isObjectSyncReady = s3Object.lastModified().plusSeconds(s3Configuration.getPolling().getSyncDelay().getSeconds())
          .isBefore(Instant.now());
    }

    return isObjectSyncReady && (isS3ObjectNew || isS3ObjectModified);
  }

  /**
   * Helper function to delete invalid registrations (deleted from s3) from our local service registrations.
   *
   * @param s3Objects The list of S3 objects to check if we need to delete from our local registrations
   */
  private void deleteInvalidRegistrations(final TransactionContext tx, final List<S3Object> s3Objects,
      S3Registry<RegistrationResource> registry) {
    Set<String> keys = s3Objects.stream().map(S3Object::key).collect(Collectors.toSet());

    // graphql-gateway/e2e/registrations/blah
    for (String cachedKey : registrationCache.keySet()) {
      if (!keys.contains(cachedKey)) {
        registrationCache.remove(cachedKey);
        registry.delete(tx, cachedKey);
      }
    }
  }

  @Getter
  @Builder
  public static class RegistrationResource {

    private final S3Object s3Object;
    private final byte[] content;

    public boolean isMainConfigFile() {
      return Predicates.isMainConfigJson.test(s3Object.key());
    }

    public boolean isSDLFile() {
      return Predicates.isSDLFile.test(s3Object.key());
    }

    public boolean isFlowFile() {
      return Predicates.isFlowFile.test(s3Object.key());
    }
  }
}
