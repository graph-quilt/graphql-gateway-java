package com.intuit.graphql.gateway.s3;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.intuit.graphql.gateway.registry.SdlServiceRegistration;
import com.intuit.graphql.gateway.registry.ServiceDefinition;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.registry.ServiceRegistration;
import org.junit.Test;

public class S3RegistrationCacheTest {


  @Test
  public void toServiceRegistrationCorrectlyConvertsSDLTypeTest() {

    ServiceDefinition sd = ServiceDefinition.newBuilder().namespace("namespace").type(Type.GRAPHQL_SDL).build();
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(sd);

    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();
    assertThat(serviceRegistration, instanceOf(SdlServiceRegistration.class));
  }

  @Test
  public void toServiceRegistrationCorrectlyConvertsGraphqlTypeTest() {

    ServiceDefinition sd = ServiceDefinition.newBuilder().namespace("namespace").type(Type.GRAPHQL).build();
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(sd);

    ServiceRegistration serviceRegistration = s3RegistrationCache.toServiceRegistration();
    assertThat(serviceRegistration, instanceOf(ServiceRegistration.class));
  }

  @Test
  public void removeResourceCorrectlyRemovesGraphqlResourceTest() {
    final String key = "graphql-gateway/dev/registrations/1.0.0/namespace/main/schema.graphqls";
    ServiceDefinition sd = ServiceDefinition.newBuilder().namespace("namespace").type(Type.GRAPHQL).build();
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(sd);

    s3RegistrationCache.addGraphqlResource(key, "schema-content");
    assertThat(s3RegistrationCache.getObjectKeyGraphqlResourceMap().get(key), notNullValue());

    s3RegistrationCache.removeResource(key);
    assertThat(s3RegistrationCache.getObjectKeyGraphqlResourceMap().get(key), nullValue());
  }

  @Test
  public void removeResourceCorrectlyRemovesFlowResourceTest() {
    final String key = "graphql-gateway/dev/registrations/1.0.0/namespace/main/service.flow";
    ServiceDefinition sd = ServiceDefinition.newBuilder().namespace("namespace").type(Type.GRAPHQL).build();
    S3RegistrationCache s3RegistrationCache = new S3RegistrationCache().setServiceDefinition(sd);

    s3RegistrationCache.addFlowResource(key, "flow-content");
    assertThat(s3RegistrationCache.getObjectKeyFlowResourceMap().get(key), notNullValue());

    s3RegistrationCache.removeResource(key);
    assertThat(s3RegistrationCache.getObjectKeyFlowResourceMap().get(key), nullValue());
  }
}
