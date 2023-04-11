package com.intuit.graphql.gateway.registry;

import static com.intuit.graphql.gateway.provider.ServiceBuilderTests.getSampleRegistryFile;
import static com.intuit.graphql.gateway.registry.ServiceRegistrationUtil.throwSpringException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.gateway.common.InvalidGatewayEnvironmentException;
import com.intuit.graphql.gateway.handler.UnprocessableEntityException;
import com.intuit.graphql.gateway.registry.ServiceDefinition.Type;
import com.intuit.graphql.gateway.s3.FileEntry;
import com.intuit.graphql.gateway.s3.S3Configuration.Region;
import com.intuit.graphql.gateway.s3.ZipUtil;
import com.intuit.graphql.orchestrator.schema.SchemaTransformationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.springframework.web.server.ServerWebInputException;

public class ServiceRegistrationUtilTests {


  @Test(expected = ConfigJsonException.class)
  public void cannotCreateSvrRegDueToMissingEnvironment() throws IOException {
    // QA is not defined
    byte[] fileInBytes = getSampleRegistryFile("mocks.registry/missing_target_endpoint.zip");
    final List<FileEntry> fileEntries = ZipUtil.uncompress(fileInBytes);
    ServiceRegistrationUtil.createServiceRegistrationFromZip(fileEntries, "QA", Region.US_WEST_2);
  }

  @Test(expected = ConfigJsonException.class)
  public void cannotCreateSvrRegDueToInvalidCfgJson() throws IOException {
    // graphqlEndpoint instead of endpoint us ised
    byte[] fileInBytes = getSampleRegistryFile("mocks.registry/invalid_registry_files.zip");
    final List<FileEntry> fileEntries = ZipUtil.uncompress(fileInBytes);
    ServiceRegistrationUtil.createServiceRegistrationFromZip(fileEntries, "QA", Region.US_WEST_2);
  }

  @Test(expected = ConfigJsonException.class)
  public void cannotCreateSvrRegDueToEmptyRegistration() throws IOException {
    byte[] fileInBytes = getSampleRegistryFile("mocks.registry/empty_registry_files.zip");
    final List<FileEntry> fileEntries = ZipUtil.uncompress(fileInBytes);
    ServiceRegistrationUtil.createServiceRegistrationFromZip(fileEntries, "QA", Region.US_WEST_2);
  }

  @Test(expected = ServiceRegistrationException.class)
  public void cannotCreateSvrRegOfTypeGraphQL() throws IOException {
    ServiceDefinition sd = ServiceDefinition.newBuilder()
        .type(Type.GRAPHQL_SDL)
        .namespace("NS")
        .endpoint("DUMMY")
        .appId("NS.Id")
        .build();

    TestHelper.createServiceRegistration(sd);
  }

  @Test(expected = ServiceRegistrationException.class)
  public void cannotCreateSvrRegOfTypeGraphQLSdl() throws IOException {
    ServiceDefinition sd = ServiceDefinition.newBuilder()
        .type(Type.GRAPHQL)
        .namespace("NS")
        .endpoint("DUMMY")
        .appId("NS.Id")
        .build();

    TestHelper.createSdlServiceRegistration(sd, new HashMap<>());
  }

  @Test
  public void getResourcePathsReturnsAllUniquePaths() {
    Map<String, String> graphqls = new HashMap<>();
    graphqls.put("a/b/c.graphql", "graphql1");
    graphqls.put("a/b/d.graphql", "graphql2");
    graphqls.put("a/f.graphql", "graphql3");

    ServiceDefinition sd = ServiceDefinition.newBuilder()
        .type(Type.GRAPHQL_SDL)
        .namespace("NS")
        .endpoint("DUMMY")
        .appId("NS.Id")
        .build();

    SdlServiceRegistration rSvcReg = SdlServiceRegistration.builder()
        .serviceDefinition(sd).graphqlResources(graphqls).build();

    final Set<String> resourcePaths = ServiceRegistrationUtil.getResourcePaths(rSvcReg);

    assertThat(resourcePaths).isNotNull();
    assertThat(resourcePaths.size()).isEqualTo(3);
    assertThat(resourcePaths.contains("a/b/c.graphql")).isTrue();
    assertThat(resourcePaths.contains("a/b/d.graphql")).isTrue();
    assertThat(resourcePaths.contains("a/f.graphql")).isTrue();

    ServiceDefinition sdl = ServiceDefinition.newBuilder()
        .type(Type.GRAPHQL_SDL)
        .namespace("NS")
        .endpoint("DUMMY")
        .appId("NS.Id")
        .build();

    SdlServiceRegistration sdlSvcReg = SdlServiceRegistration.builder()
        .serviceDefinition(sdl).graphqlResources(graphqls).build();

    final Set<String> sdlResourcePaths = ServiceRegistrationUtil.getResourcePaths(sdlSvcReg);

    assertThat(resourcePaths).isNotNull();
    assertThat(sdlResourcePaths.size()).isEqualTo(3);
    assertThat(resourcePaths.contains("a/b/c.graphql")).isTrue();
    assertThat(resourcePaths.contains("a/b/d.graphql")).isTrue();
    assertThat(resourcePaths.contains("a/f.graphql")).isTrue();

    ServiceDefinition graphql = ServiceDefinition.newBuilder()
        .type(Type.GRAPHQL)
        .namespace("NS")
        .endpoint("DUMMY")
        .appId("NS.Id")
        .build();

    ServiceRegistration svcReg = ServiceRegistration.baseBuilder()
        .serviceDefinition(graphql).build();

    final Set<String> graphqlResourcePaths = ServiceRegistrationUtil.getResourcePaths(svcReg);
    assertThat(graphqlResourcePaths).isNotNull();
    assertThat(graphqlResourcePaths.size()).isEqualTo(0);
  }

  private static final String reason = "Random reason";

  @Test
  public void throws422OnServiceRegistrationException() {
    assertThatThrownBy(
        () -> throwSpringException(new ServiceRegistrationException(reason)))
        .isInstanceOf(
            UnprocessableEntityException.class).hasMessageContaining(reason);
  }

  @Test
  public void throws422OnNestedStitchingException() {
    assertThatThrownBy(
        () -> throwSpringException(new ServiceRegistrationException("some other reasons",
            new SchemaTransformationException(reason))))
        .isInstanceOf(
            UnprocessableEntityException.class).hasMessageContaining(reason);
  }

  @Test
  public void throws400OnInvalidGatewayEnvironmentException() {
    assertThatThrownBy(() -> throwSpringException(new InvalidGatewayEnvironmentException(reason))).isInstanceOf(
        ServerWebInputException.class).hasMessageContaining(reason);
  }

}
