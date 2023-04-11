package com.intuit.graphql.gateway.registry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.graphql.RuntimeGraphBuilder;
import com.intuit.graphql.gateway.graphql.SchemaManager;
import com.intuit.graphql.gateway.provider.ServiceBuilder;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.orchestrator.batch.BatchLoaderExecutionHooks;
import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import graphql.schema.GraphQLSchema;
import graphql.schema.diff.DiffCategory;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.Arrays;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@RunWith(MockitoJUnitRunner.class)
public class SchemaValidationServiceTests {

  private SchemaValidationService schemaValidationService;

  private String firstAppSchema = "type Query { person : Person personById(id: ID) : Person } "
      + "type Person { id: ID name: String address: Address bookId: ID income: Int debt: Int } "
      + "type Address { id: ID street: String city: String zip: String state: String country: String } ";

  private String newAppSchema = "type Query { bookById(id: ID): Book books : [Book] } "
      + "type Book { id: ID name: String pageCount: Int author: Author } "
      + "type Author { id: ID firstName: String lastName: String } ";

  private String newAppInvalidSchema = "type Query { bookById(id: ID): Book books : [Book] } "
      + "type Book { id: ID name: String pageCount: Int author: Author } "
      + "type Person { id: ID name: String bookId: ID income: Int debt: Int } "
      // type conflict with app2
      + "type Author { id: ID firstName: String lastName: String } ";

  private String firstAppUpdatedSchema =
      "type Query { person : Person personById(id: ID) : Person newTopLevelField : Person } "
          + "type Person { id: ID name: String address: Address income: Int debt: Int } "
          + "type Address { id: ID street: String city: String zip: String state: String  } ";

  @Mock
  WebClient webClient;

  @Mock
  SchemaManager schemaManager;

  @Mock
  RuntimeGraph runtimeGraph;

  @Before
  public void setup() {
    ServiceBuilder serviceBuilder = new ServiceBuilder(webClient);
    RuntimeGraphBuilder runtimeGraphBuilder = new RuntimeGraphBuilder(serviceBuilder,
        mock(BatchLoaderExecutionHooks.class));
    ServiceRegistration firstapp = TestHelper.createTestSDLRegistration(firstAppSchema, "FirstApp.Id", "FIRSTAPP",
        Type.GRAPHQL_SDL);
    when(runtimeGraph.getExecutableSchema()).thenReturn(buildSchema(firstAppSchema));
    when(schemaManager.getCachedRegistrations()).thenReturn(Flux.fromIterable(Arrays.asList(firstapp)));
    when(schemaManager.getRuntimeGraph()).thenReturn(runtimeGraph);
    schemaValidationService = new SchemaValidationService(schemaManager, runtimeGraphBuilder);
  }

  private GraphQLSchema buildSchema(String sdl) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
    return new SchemaGenerator().makeExecutableSchema(typeRegistry, RuntimeWiring.newRuntimeWiring().build());
  }

  @Test
  public void canGetToBeRegistrationNewSDLRegistration() {
    // test case for a new SDL Based service (does not exists in cached registrations)
    ServiceRegistration newReg = TestHelper.createTestSDLRegistration(newAppSchema, "NewApp.Id", "NEWAPP",
        Type.GRAPHQL_SDL);

    Flux<ServiceRegistration> toBeRegistartions = schemaValidationService
        .getToBeRegistrations((SdlServiceRegistration) newReg);

    // since this a new provider, a toBeRegistartions will be returned.  toBeRegistrations
    // should include the old regisration + new one
    StepVerifier.create(toBeRegistartions)
        .expectNextMatches(
            serviceRegistration -> serviceRegistration.getServiceDefinition().getAppId()
                .equals("FirstApp.Id"))
        .expectNextMatches(
            serviceRegistration -> serviceRegistration.getServiceDefinition().getAppId()
                .equals("NewApp.Id"))
        .expectComplete()
        .verify();
  }

  @Test
  public void canGetToBeRegistrationWithSchemaUpdate() {
    // test case for a existing service with new changes in schema
    ServiceRegistration newRegistration = TestHelper.createTestSDLRegistration(firstAppUpdatedSchema,
        "FirstApp.Id", "FIRSTAPP", Type.GRAPHQL_SDL);

    Flux<ServiceRegistration> toBeRegistartions = schemaValidationService
        .getToBeRegistrations((SdlServiceRegistration) newRegistration);

    // since there's an update, a toBeRegistartions will be returned.
    StepVerifier.create(toBeRegistartions)
        .expectNextMatches(
            serviceRegistration -> serviceRegistration.getServiceDefinition().getAppId()
                .equals("FirstApp.Id"))
        .expectComplete()
        .verify();
  }

  @Test
  public void canGetToBeRegistrationNoSchemaUpdate() {
    // test case for a existing service with NO changes in schema
    // FirstApp.Id is pre-registered with firstAppSchema
    ServiceRegistration newRegistration = TestHelper.createTestSDLRegistration(firstAppSchema, "FirstApp.Id",
        "FIRSTAPP",
        Type.GRAPHQL_SDL);

    Flux<ServiceRegistration> toBeRegistartions = schemaValidationService
        .getToBeRegistrations(newRegistration);

    StepVerifier.create(toBeRegistartions)
        .verifyComplete(); // means no exception
  }

  @Test
  public void validateSDLRegistration() {
    ServiceRegistration newRegistration = TestHelper.createTestSDLRegistration(newAppSchema, "NewApp.Id",
        "NEWAPP", Type.GRAPHQL_SDL);

    StepVerifier.create(schemaValidationService.validate(newRegistration))
        .expectNextMatches(schemaDifferenceMetrics ->
                Objects.nonNull(schemaDifferenceMetrics) &&
                    Objects.nonNull(schemaDifferenceMetrics.getServiceRegistration())
                && schemaDifferenceMetrics.getInfos().size()==2 &&
                    schemaDifferenceMetrics.getInfos().get(0).getCategory() == DiffCategory.ADDITION
                 )
        .verifyComplete(); // means no exception
  }

  @Test
  public void validateSameServiceSDLRegistration() {
    ServiceRegistration newRegistration = TestHelper.createTestSDLRegistration(firstAppUpdatedSchema, "FirstApp.Id",
        "FIRSTAPP", Type.GRAPHQL_SDL);

    StepVerifier.create(schemaValidationService.validate(newRegistration))
        .expectNextMatches(schemaDifferenceMetrics ->
            Objects.nonNull(schemaDifferenceMetrics) &&
                Objects.nonNull(schemaDifferenceMetrics.getServiceRegistration())
                && schemaDifferenceMetrics.getInfos().size()==1 &&
                schemaDifferenceMetrics.getInfos().get(0).getCategory() == DiffCategory.ADDITION
                && schemaDifferenceMetrics.getBreakages().size()==2 &&
                schemaDifferenceMetrics.getBreakages().get(0).getCategory() == DiffCategory.MISSING
        )
        .verifyComplete(); // means no exception
  }


  @Test
  public void cannotStitchNewRegistrationWithInvalidSchema() {
    ServiceRegistration invalidRegistration = TestHelper.createTestSDLRegistration(newAppInvalidSchema,
        "NewApp.Id", "NEWAPP", Type.GRAPHQL_SDL);

    StepVerifier.create(schemaValidationService.validate(invalidRegistration))
        .expectErrorMatches(throwable -> throwable instanceof ServiceRegistrationException)
        .verify();
  }
}
