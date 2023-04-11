package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.logging.ContextFactory;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.SubtaskContext;
import com.intuit.graphql.gateway.provider.ServiceBuilder;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.webclient.TxProvider;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Component
@Slf4j
public class RuntimeGraphBuilder {

  private final ServiceBuilder serviceBuilder;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderExecutionHooks;

  public RuntimeGraphBuilder(final ServiceBuilder serviceBuilder,
      final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> batchLoaderExecutionHooks) {
    this.serviceBuilder = serviceBuilder;
    this.batchLoaderExecutionHooks = batchLoaderExecutionHooks;
  }

  /**
   * Build the {@link RuntimeGraph} object by stitching individual service schemas.
   *
   * @param serviceRegistrations Flux of {@link ServiceRegistration}
   * @return Stitched/Merged graphql orchestrator instance to be used to make queries
   */
  public Mono<RuntimeGraph> build(Flux<ServiceRegistration> serviceRegistrations) {
    return build(serviceRegistrations, false);
  }

  public Mono<RuntimeGraph> build(Flux<ServiceRegistration> serviceRegistrations, boolean s3Registration) {
    return TxProvider.embeddedTx().flatMap(tx -> {
      SubtaskContext subtaskContext = ContextFactory.getSubtaskContext("Stitching schemas");
      return Flux.defer(() -> serviceRegistrations)
          .doOnSubscribe(x -> EventLogger.subtaskStart(log, tx, subtaskContext))
          .doOnNext(serviceRegistration -> EventLogger
              .info(log, tx, "Registering provider",
                  ImmutableLogNameValuePair.of("namespace", serviceRegistration.getServiceDefinition().getNamespace())))
          .parallel()
          .runOn(Schedulers.elastic()) // use thread pool since we can't block on a netty thread
          .map(serviceRegistration -> serviceBuilder.buildService(tx, serviceRegistration))
          .sequential()
          .onErrorContinue(Predicates.isSkippableRegistrationError(s3Registration), (t, o) -> EventLogger.error(log, tx, "Ignoring provider", t))
          .reduce(SchemaStitcher.newBuilder(), SchemaStitcher.Builder::service)
          .map(builder -> builder.batchLoaderHooks(batchLoaderExecutionHooks)
              .build()
              .stitchGraph()
          )
          .doOnSuccess(x -> EventLogger.info(log, tx, "Schema stitch ok"))
          .doOnError(err -> EventLogger.error(log, tx, "Error stitching schemas", err))
          .doFinally(x -> EventLogger.subtaskEnd(log, tx, subtaskContext));
    });
  }
}
