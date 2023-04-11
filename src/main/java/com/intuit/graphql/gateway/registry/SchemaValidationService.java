package com.intuit.graphql.gateway.registry;

import static com.intuit.graphql.gateway.registry.ServiceRegistrationUtil.isNotSameSchema;
import static com.intuit.graphql.gateway.registry.ServiceRegistrationUtil.isSameService;

import com.intuit.graphql.gateway.graphql.RuntimeGraphBuilder;
import com.intuit.graphql.gateway.graphql.SchemaManager;
import com.intuit.graphql.gateway.handler.UnprocessableEntityException;
import com.intuit.graphql.gateway.registry.SchemaDifferenceMetrics.SchemaDifferenceMetricsBuilder;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import graphql.schema.GraphQLSchema;
import graphql.schema.diff.DiffSet;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.reporting.CapturingReporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SchemaValidationService {

  @Getter
  private SchemaManager schemaManager;
  private RuntimeGraphBuilder runtimeGraphBuilder;

  public SchemaValidationService(SchemaManager schemaManager, RuntimeGraphBuilder runtimeGraphBuilder) {
    this.schemaManager = schemaManager;
    this.runtimeGraphBuilder = runtimeGraphBuilder;
  }

  public Mono<SchemaDifferenceMetrics> validate(ServiceRegistration newSvcReg) {
    ServiceDefinition.Type type = ServiceRegistrationUtil.getServiceDefinitionType(newSvcReg);
    Objects.requireNonNull(type);
    switch (type) {
      case GRAPHQL_SDL:
      case REST:
        return canStitch(newSvcReg);
      case GRAPHQL:
        return Mono.just(SchemaDifferenceMetrics.builder().serviceRegistration(newSvcReg).build());
      default:
        return Mono.error(new UnprocessableEntityException(type + " service definition type is not supported."));
    }
  }

  /**
   * Tests if the given set of {@link ServiceRegistration} can be stitched or not.
   *
   * @param newSvcReg Producer of {@link ServiceRegistration} set to be tested
   * @return SchemaDifferenceMetrics
   */
  private Mono<SchemaDifferenceMetrics> canStitch(ServiceRegistration newSvcReg) {
    return Mono.just(getToBeRegistrations(newSvcReg))
        .flatMap(runtimeGraphBuilder::build)
        .map(runtimeGraph -> getSchemaDifference(runtimeGraph, newSvcReg))
        .onErrorResume(throwable -> {
          throw new ServiceRegistrationException(throwable.getMessage(), throwable);
        });
  }

  private SchemaDifferenceMetrics getSchemaDifference(RuntimeGraph runtimeGraph, ServiceRegistration newSvcReg) {
    SchemaDifferenceMetricsBuilder schemaDifferenceMetricsBuilder = SchemaDifferenceMetrics.builder()
        .serviceRegistration(newSvcReg);
    final GraphQLSchema oldSchema = schemaManager.getRuntimeGraph().getExecutableSchema();
    final GraphQLSchema newSchema = runtimeGraph.getExecutableSchema();
    CapturingReporter capturingReporter = new CapturingReporter();
    new SchemaDiff().diffSchema(DiffSet.diffSet(oldSchema, newSchema), capturingReporter);
    schemaDifferenceMetricsBuilder = schemaDifferenceMetricsBuilder
        .breakages(capturingReporter.getBreakages())
        .infos(capturingReporter.getInfos().stream().filter(event -> Objects.nonNull(event.getCategory())).collect(
            Collectors.toList()))
        .dangers(capturingReporter.getDangers());
    return schemaDifferenceMetricsBuilder.build();
  }

  /**
   * Creates Flux of service registration. The flux will emit objects from cached service registrations and possibly
   * replace if an incoming SdlServiceRegistrations exists in cache and has an schema update.
   *
   * @param incomingRegistration Incoming Service Registration
   * @return Producer of new set of {@link ServiceRegistration}
   */
  protected Flux<ServiceRegistration> getToBeRegistrations(ServiceRegistration incomingRegistration) {
    return schemaManager.getCachedRegistrations()
        .reduce(new ToBeServiceRegistrations(),
            (toBeSvcRegistrations, existingRegistration) -> {
              // check if appId's are the same (i.e. if same service)
              if (isSameService(existingRegistration, incomingRegistration)) {
                toBeSvcRegistrations.setIncomingMatched(true);
                if (isNotSameSchema(existingRegistration, incomingRegistration)) {
                  toBeSvcRegistrations.setSchemaUpdated(true);
                  toBeSvcRegistrations.add(incomingRegistration);
                } else {
                  toBeSvcRegistrations.add(existingRegistration);
                }
              } else {
                toBeSvcRegistrations.add(existingRegistration);
              }
              return toBeSvcRegistrations;
            }
        ).flatMapMany(toBeServiceRegistrations -> {
          if (!toBeServiceRegistrations.isIncomingMatched()) {
            // incoming NOT matched
            toBeServiceRegistrations.add(incomingRegistration);
            toBeServiceRegistrations.setSchemaUpdated(true);
          }
          if (toBeServiceRegistrations.isSchemaUpdated()) {
            return Flux.fromIterable(toBeServiceRegistrations.getServiceRegistrations());
          }
          return Flux.empty(); // there's no schema update, hence no "to-be"
        });
  }

  /**
   * This class holds set of service registrations (at most one incoming) that will be tested if they can stitched
   * together or not.
   */
  @Data
  public static class ToBeServiceRegistrations {

    private List<ServiceRegistration> serviceRegistrations = new ArrayList<>();
    private boolean incomingMatched;
    private boolean schemaUpdated;

    public void add(ServiceRegistration registration) {
      serviceRegistrations.add(registration);
    }
  }
}
