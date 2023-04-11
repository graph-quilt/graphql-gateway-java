package com.intuit.graphql.gateway;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.intuit.graphql.gateway.config.properties.AppLoggingProperties;
import com.intuit.graphql.gateway.config.properties.AppSecurityProperties;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceRegistrationException;
import com.intuit.graphql.gateway.registry.ServiceRegistrationUtil;
import com.intuit.graphql.gateway.s3.ImmutableEnvironmentSpecification;
import com.intuit.graphql.gateway.s3.ImmutableS3ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.GatewayEnvironment;
import com.intuit.graphql.gateway.webclient.TxProvider;
import com.intuit.graphql.gateway.s3.S3ServiceDefinition.EnvironmentSpecification;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

public class TestHelper {

  /**
   * Dummy endpoint needed to create {@link ServiceRegistration}.  Running grapqhl queries is not needed for this schema
   * validation testing.
   */
  private static final String DUMMY_ENDPT = "http://localhost:4444/graphql";

  /**
   * Provides a default implementation of the TransactionContext to be used in tests.
   */
  public static TransactionContext testTx() {
    return ImmutableTransactionContext.builder().build();
  }

  public static TxProvider testTxProvider() {
    return new TxProvider(new AppSecurityProperties(), new AppLoggingProperties());
  }

  @SneakyThrows
  public static byte[] read(String path) {
    return Resources.toByteArray(Resources.getResource(path));
  }

  public static String readString(String path) {
    return new String(read(path));
  }

  /**
   * Creates {@link ServiceRegistration} with graphql resources.  It is expected that the given {@link
   * ServiceDefinition} is of Type {@link ServiceDefinition.Type#GRAPHQL_SDL}
   *
   * @param serviceDefinition Service Definition
   * @param graphqlsResources GraphQL Resources
   * @return {@link ServiceRegistration}
   */
  public static ServiceRegistration createSdlServiceRegistration(ServiceDefinition serviceDefinition,
      Map<String, String> graphqlsResources) {
    if (serviceDefinition.getType() != Type.GRAPHQL_SDL) {
      throw new ServiceRegistrationException("Error creating SdlServiceRegistration.  Expecting GRAPHQL_SDL type");
    }
    return ServiceRegistrationUtil.createServiceRegistration(serviceDefinition, null, graphqlsResources);
  }

  /**
   * Creates {@link ServiceRegistration} with no flow or graphql resources.  It is expexted that the given {@link
   * ServiceDefinition} is of Type {@link ServiceDefinition.Type#GRAPHQL}
   *
   * @param serviceDefinition Service Definition
   * @return {@link ServiceRegistration}
   */
  public static ServiceRegistration createServiceRegistration(ServiceDefinition serviceDefinition) {
    if (serviceDefinition.getType() != Type.GRAPHQL) {
      throw new ServiceRegistrationException("Error creating ServiceRegistration.  Expecting GRAPHQL type");
    }
    return ServiceRegistrationUtil.createServiceRegistration(serviceDefinition, null, null);
  }

  public static ServiceRegistration createTestSDLRegistration(String schema, String appId, String namespace, Type type) {
    return TestHelper.createTestRegistration(appId, namespace, type, ImmutableMap.of("schema.graphqls",schema), null );
  }

  public static ServiceRegistration createTestRegistration( String appId, String namespace, Type type, Map<String, String> sdlResources,
      Map<String, String> flowResources) {

    // CREATE APP ENVIRONMEWNTS.  For this test only 1 as QA
    Map<GatewayEnvironment, EnvironmentSpecification> appEnvs = new HashMap<>();
    appEnvs.put(GatewayEnvironment.QA, ImmutableEnvironmentSpecification
        .builder().endpoint(DUMMY_ENDPT).build()
    );

    // CREATE S3ServiceDefinition - same with config.json content
    S3ServiceDefinition firstAppServiceDefinition = ImmutableS3ServiceDefinition.builder()
        .appId(appId)
        .namespace(namespace)
        .environments(appEnvs)
        .type(type)
        .build();

    return ServiceRegistrationUtil
        .createServiceRegistration(firstAppServiceDefinition.toServiceDefinition("QA",  Region.US_WEST_2), flowResources, sdlResources);
  }

  /**
   * This default server context should be used for a mock exchange.
   * This should cover most cases, if not you will need to implement a one-off
   */
  public static final ServerResponse.Context DEFAULT_SERVER_CONTEXT = new ServerResponse.Context() {
    @Override
    public List<HttpMessageWriter<?>> messageWriters() {
      return HandlerStrategies.withDefaults().messageWriters();
    }

    @Override
    public List<ViewResolver> viewResolvers() {
      return Collections.emptyList();
    }
  };
}