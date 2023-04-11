package com.intuit.graphql.gateway.s3;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.StringJoiner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class SchemaRegistrationS3ClientTest {


  private SchemaRegistrationS3Client client;
  @Mock
  private ReactorS3Client reactorS3Client;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    final S3Configuration s3Configuration = new S3Configuration();
    s3Configuration.setAppName("graphql-gateway");
    s3Configuration.getPolling().setBucketname("test-bucket");
    s3Configuration.setVersion("1.0.0");
    s3Configuration.setEnv("e2e");
    client = new SchemaRegistrationS3Client(reactorS3Client, s3Configuration);
    when(reactorS3Client.s3ApplicationPrefix()).thenReturn(new StringJoiner("/").add("graphql-gateway").add("e2e"));
  }

  @Test
  public void testListRegistrationsWithNoMainFolder() {
    when(reactorS3Client.listObjects(eq("graphql-gateway/e2e/registrations/1.0.0")))
        .thenReturn(Mono.just(ListObjectsV2Response.builder()
            .contents(
                S3Object.builder().key("graphql-gateway/e2e/registrations/1.0.0/test-registration/config.json").build())
            .build()));

    StepVerifier.create(client.listRegistrations())
        .expectNextCount(0).verifyComplete();
  }

  @Test
  public void testListRegistrationsWithMainFolder() {
    when(reactorS3Client.listObjects(eq("graphql-gateway/e2e/registrations/1.0.0")))
        .thenReturn(Mono.just(ListObjectsV2Response.builder()
            .contents(
                S3Object.builder().key("graphql-gateway/e2e/registrations/1.0.0/test-registration/main/config.json").build())
            .build()));

    StepVerifier.create(client.listRegistrations())
        .expectNextCount(1).verifyComplete();
  }

  @Test
  public void testListRegistrationsWithTestFolder() {
    when(reactorS3Client.listObjects(eq("graphql-gateway/e2e/registrations/1.0.0")))
        .thenReturn(Mono.just(ListObjectsV2Response.builder()
            .contents(
                S3Object.builder().key("graphql-gateway/e2e/registrations/1.0.0/test-registration/test/some-karate.feature")
                    .build())
            .build()));
    StepVerifier.create(client.listRegistrations())
        .expectNextCount(0).verifyComplete();
  }

  @Test
  public void testListRegistrationsWithNullKey() {
    when(reactorS3Client.listObjects(eq("graphql-gateway/e2e/registrations/1.0.0")))
        .thenReturn(Mono.just(ListObjectsV2Response.builder()
            .contents(
                S3Object.builder().key(null).build())
            .build()));

    StepVerifier.create(client.listRegistrations())
        .expectNextCount(0).verifyComplete();
  }
}

