package com.intuit.graphql.gateway.registry;

import com.google.common.annotations.VisibleForTesting;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.config.properties.WebClientProperties;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.s3.RegistrationPoller.RegistrationResource;
import com.intuit.graphql.gateway.s3.S3Configuration;
import com.intuit.graphql.gateway.s3.S3Poller;
import com.intuit.graphql.gateway.s3.S3RegistrationCache;
import com.intuit.graphql.gateway.s3.S3Registry;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition;
import com.intuit.graphql.gateway.webclient.TxProvider;
import io.vavr.control.Try;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Handles {@link S3RegistrationCache} clients read from an S3 bucket. Given an S3 bucket specified in the {@link
 * S3Configuration}, we listen to changes and inform the {@link ServiceRegistry} of any changes so it can rebuild the
 * schema.
 */
@Configuration
@Slf4j
public class S3ServiceRegistrationProvider implements S3Registry<RegistrationResource>, ServiceRegistrationProvider {

  private static final String SERVICE_DEFINITION_MESSAGE = "Service definition downloaded";
  private static final String GRAPHQL_RESOURCE_MESSAGE = "Graphql resource downloaded";
  private static final String FLOW_RESOURCE_MESSAGE = "Flow resource downloaded";
  private static final String REMOVED_RESOURCE_MESSAGE = "Resource deleted from cache";

  private final S3Poller<RegistrationResource> poller;
  private final S3Configuration s3Configuration;
  private final TxProvider txProvider;
  private final WebClientProperties webClientProperties;

  /**
   * Provides constant-time lookup for providers based on their namespace.
   */
  @VisibleForTesting
  Map<String, S3RegistrationCache> regIdS3RegistrationCacheMap;
  Map<String, RegistrationResource> resourceMap;

  private ServiceRegistry serviceRegistry;

  /**
   * Create a new {@link S3ServiceRegistrationProvider} based on an {@link S3Poller} and {@link S3Configuration}.
   *
   * @param poller The poller used to interact with S3 and retrieve updates
   * @param s3Configuration The S3 configuration used to specify which bucket we are reading
   * @param txProvider Transaction provider to use for logging
   * @param webClientProperties The custom properties for webclient
   */
  public S3ServiceRegistrationProvider(final S3Poller<RegistrationResource> poller,
      final S3Configuration s3Configuration,
      TxProvider txProvider, WebClientProperties webClientProperties) {
    this.poller = poller;
    this.s3Configuration = s3Configuration;
    this.txProvider = txProvider;
    this.regIdS3RegistrationCacheMap = new ConcurrentHashMap<>();
    this.resourceMap = new ConcurrentHashMap<>();
    this.webClientProperties = webClientProperties;
  }

  @Override
  public boolean isEmpty() {
    return regIdS3RegistrationCacheMap.isEmpty();
  }

  /**
   * Returns reactive sequence that can be `subscribed to` to trigger a new build of graphql orchestrator with the
   * latest {@link ServiceRegistration}s from local cache.
   *
   * @return Reactive Sequence to trigger GraphQLOrchestrator build he latest {@link ServiceRegistration}s
   */
  @Override
  public Mono<Void> update() {
    return serviceRegistry.updateRegistry(getRegistryId(), getServiceRegistrationFlux());
  }

  @Override
  public RegistrationResource cache(final TransactionContext tx, final RegistrationResource registrationResource) {
    final String objectKey = registrationResource.getS3Object().key();
    final String registrationId = extractRegistrationIdFromKey(registrationResource.getS3Object().key());

    if (registrationResource.isMainConfigFile()) {
      this.cacheServiceDefinition(tx, registrationId, parseServiceDefinition(registrationResource.getContent(), tx));
    } else if (registrationResource.isFlowFile()) {
      this.cacheFlowResource(tx, registrationId, objectKey, new String(registrationResource.getContent()));
    } else if (registrationResource.isSDLFile()) {
      this.cacheGraphqlResource(tx, registrationId, objectKey, new String(registrationResource.getContent()));
    }

    this.resourceMap.put(registrationId, registrationResource);
    return registrationResource;
  }

  @Override
  public void delete(final TransactionContext tx, final String key) {
    final String registrationId = extractRegistrationIdFromKey(key);
    Optional.ofNullable(regIdS3RegistrationCacheMap.get(registrationId))
        .ifPresent(s3Registration -> {
          if (Predicates.isMainConfigJson.test(key)) {
            regIdS3RegistrationCacheMap.remove(registrationId);
          } else {
            s3Registration.removeResource(key);
          }
          EventLogger.info(log, tx, REMOVED_RESOURCE_MESSAGE, loggableResourceFields(registrationId, key));
        });
  }

  @Override
  public RegistrationResource get(final String key) {
    return this.resourceMap.get(key);
  }

  /**
   * Get the registration ID from the S3 object key.
   *
   * @param s3Key The s3 object key
   * @return The registration ID contained in the key
   */
  private String extractRegistrationIdFromKey(final String s3Key) {
    final String[] pathVars = s3Key.split("/");
    return pathVars[4];
  }

  /**
   * Parse the contents of config JSON file into {@link ServiceDefinition} based on the target environment defined in
   * {@link S3Configuration}.
   *
   * @param contents the config JSON file as bytes
   * @return The {@link ServiceDefinition} parsed from the contents of config JSON file and the target environment. This
   * value may be null if we fail to get the {@link ServiceDefinition}
   */
  private ServiceDefinition parseServiceDefinition(final byte[] contents, TransactionContext tx) {
    return Try.of(() -> {
          S3ServiceDefinition s3ServiceDefinition = Mapper.mapper().readValue(contents, S3ServiceDefinition.class);
          ServiceDefinition serviceDefinition = s3ServiceDefinition
              .toServiceDefinition(s3Configuration.getEnv(), s3Configuration.getRegion());
          serviceDefinition.setTimeout(Math.min(webClientProperties.getTimeout(), serviceDefinition.getTimeout()));
          return serviceDefinition;
        }
    ).onFailure(e -> EventLogger.error(log, tx, "Failed to parse service definition", e))
        .getOrNull();
  }


  /**
   * This method allows provider service definition to be asynchronously fetched and registered, independent of the
   * normal registration lifecycle.
   *
   * @param tx a transaction context for logging
   * @param registrationId a unique registration name
   * @param serviceDefinition the provider configuration to sync
   */
  private void cacheServiceDefinition(TransactionContext tx, String registrationId,
      ServiceDefinition serviceDefinition) {
    final S3RegistrationCache s3RegistrationCache = regIdS3RegistrationCacheMap
        .computeIfAbsent(registrationId, id -> new S3RegistrationCache());

    s3RegistrationCache.setServiceDefinition(serviceDefinition);
    regIdS3RegistrationCacheMap.put(registrationId, s3RegistrationCache);

    EventLogger.info(log, tx, SERVICE_DEFINITION_MESSAGE, loggableProviderFields(registrationId, serviceDefinition));
  }

  /**
   * This method allows providers to be asynchronously fetched and registered, independent of the normal registration
   * lifecycle.
   *
   * @param tx a transaction context for logging
   * @param registrationId a unique registration name
   * @param key s3 object key
   * @param schemaStr the provider configuration to sync
   */
  private void cacheGraphqlResource(TransactionContext tx, String registrationId, String key, String schemaStr) {
    final S3RegistrationCache s3RegistrationCache = regIdS3RegistrationCacheMap
        .computeIfAbsent(registrationId, id -> new S3RegistrationCache());

    s3RegistrationCache.addGraphqlResource(key, schemaStr);
    EventLogger.info(log, tx, GRAPHQL_RESOURCE_MESSAGE, loggableResourceFields(registrationId, key));
  }

  /**
   * This method allows providers to be asynchronously fetched and registered, independent of the normal registration
   * lifecycle.
   *
   * @param tx a transaction context for logging
   * @param registrationId a unique registration name
   * @param key s3 object key
   * @param source the provider configuration to sync
   */
  private void cacheFlowResource(TransactionContext tx, String registrationId, String key, String source) {
    final S3RegistrationCache s3RegistrationCache = regIdS3RegistrationCacheMap
        .computeIfAbsent(registrationId, id -> new S3RegistrationCache());
    s3RegistrationCache.addFlowResource(key, source);
    EventLogger.info(log, tx, FLOW_RESOURCE_MESSAGE, loggableResourceFields(registrationId, key));
  }

  /**
   * Fetch initial {@link ServiceDefinition}s and start the polling sequence that listens for updates.
   */
  @PostConstruct
  public void beginPolling() {
    if (!s3Configuration.isEnabled()) {
      return;
    }
    if (s3Configuration.getPolling().isEnabled()) {
      Mono.just(poller)
          .doOnNext(poll -> Mono.defer(() -> poll.buildPollingSequence(this)
              .delaySubscription(s3Configuration.getPolling().getPeriod(), poll.getDefaultScheduler()))
              .onErrorResume(e -> Mono.empty())
              .repeat(() -> s3Configuration.getPolling().isEnabled())
              .subscribe()
          ).doOnError(e -> EventLogger
          .error(log, txProvider.newTx(),
              "Failed to start polling sequence!", e))
          .subscribe();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<ServiceRegistration> getInitialServiceRegistrations() {
    return TxProvider.embeddedTx().flatMapMany(tx ->
        this.poller.fetch(this, tx)
            .thenMany(getServiceRegistrationFlux())
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Returns a Flux of {@link ServiceRegistration}s from local Cache (downloaded from S3)
   *
   * @return Flux of ServiceRegistrations from Cache
   */
  private Flux<ServiceRegistration> getServiceRegistrationFlux() {
    return Flux.fromIterable(regIdS3RegistrationCacheMap.values())
        .filter(s3RegistrationCache -> Objects.nonNull(s3RegistrationCache.getServiceDefinition()))
        .map(S3RegistrationCache::toServiceRegistration);
  }

  /**
   * Return a loggable version of the S3 object registration ID and the {@link ServiceDefinition} associated with it.
   *
   * @param registrationId The s3 object registration ID. Note that this is NOT the {@link #getRegistryId()}.
   * @param sd The service definition to log
   * @return An array of loggable provider fields
   */
  private LogNameValuePair[] loggableProviderFields(final String registrationId, final ServiceDefinition sd) {
    return new ImmutableLogNameValuePair[]{
        ImmutableLogNameValuePair.of("registrationId", registrationId),
        ImmutableLogNameValuePair.of("serviceDefinition", sd == null ? null : sd.createEventLoggerFields())
    };
  }

  private LogNameValuePair[] loggableResourceFields(final String registrationId, final String key) {
    return new LogNameValuePair[]{
        ImmutableLogNameValuePair.of("registrationId", registrationId),
        ImmutableLogNameValuePair.of("objectKey", key)
    };
  }
}
