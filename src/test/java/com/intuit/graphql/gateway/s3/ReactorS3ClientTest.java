package com.intuit.graphql.gateway.s3;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

public class ReactorS3ClientTest {

  @Mock
  public S3AsyncClient asyncClient;

  public ReactorS3Client clientUnderTest;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    final S3Configuration s3Configuration = new S3Configuration();
    s3Configuration.getPolling().setBucketname("test-bucket");
    clientUnderTest = new ReactorS3Client(asyncClient, s3Configuration);
  }

  @Test
  public void testRetryPropagatesError() {
    CompletableFuture<ListObjectsV2Response> failedFuture = new CompletableFuture<>();
    failedFuture
        .completeExceptionally(new CompletionException(S3Exception.builder().statusCode(404).build()));

    doReturn(failedFuture).when(asyncClient).listObjectsV2(any(Consumer.class));

    StepVerifier.create(clientUnderTest.listObjects("/some/prefix"))
        .consumeErrorWith(throwable -> assertThat(throwable).isInstanceOf(S3Exception.class))
        .verify();
  }

  @Test
  public void testRetrySucceeds() {
    CompletableFuture<ListObjectsV2Response> failedFuture = new CompletableFuture<>();
    failedFuture
        .completeExceptionally(new CompletionException(S3Exception.builder().statusCode(500).build()));

    when(asyncClient.listObjectsV2(any(Consumer.class)))
        .thenReturn(failedFuture, failedFuture,
            completedFuture(ListObjectsV2Response.builder().contents(S3Object.builder().build()).build()));

    StepVerifier.create(clientUnderTest.listObjects("/some/prefix"))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  public void testRetryMaxAttempts() {
    CompletableFuture<ListObjectsV2Response> failedFuture = new CompletableFuture<>();
    failedFuture
        .completeExceptionally(new CompletionException(S3Exception.builder().statusCode(500).build()));

    when(asyncClient.listObjectsV2(any(Consumer.class)))
        .thenReturn(failedFuture, failedFuture, failedFuture, failedFuture);

    StepVerifier.create(clientUnderTest.listObjects("/some/prefix"))
        .consumeErrorWith(throwable -> assertThat(throwable).isInstanceOf(S3Exception.class))
        .verify();
  }

  @Test
  public void testGetObject() {
    verifyGetObjectRequestBuilder(getObjectRequest -> {
          assertThat(getObjectRequest.key()).isEqualTo("some/key");
          assertThat(getObjectRequest.bucket()).isEqualTo("test-bucket");
        },
        completedFuture(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[1]))
    );

    StepVerifier.create(clientUnderTest.getObject("some/key"))
        .consumeNextWith(responseBytes -> assertThat(responseBytes.asByteArray().length).isEqualTo(1))
        .verifyComplete();
  }

  @Test
  public void testListObjects() {
    verifyListObjectsRequestBuilder(listObjectsV2Request -> {
          assertThat(listObjectsV2Request.prefix()).isEqualTo("some/prefix");
          assertThat(listObjectsV2Request.bucket()).isEqualTo("test-bucket");
        },
        completedFuture(ListObjectsV2Response.builder().name("test-response").build())
    );

    StepVerifier.create(clientUnderTest.listObjects("some/prefix"))
        .consumeNextWith(response -> assertThat(response.name()).isEqualTo("test-response"))
        .verifyComplete();
  }

  private void verifyListObjectsRequestBuilder(Consumer<ListObjectsV2Request> requestAssertions,
      CompletableFuture<ListObjectsV2Response> mockResponse) {
    when(asyncClient.listObjectsV2(any(Consumer.class))) //todo
        .thenAnswer(invocation -> {
          Consumer<ListObjectsV2Request.Builder> consumer = invocation.getArgument(0);
          ListObjectsV2Request.Builder b = ListObjectsV2Request.builder();
          consumer.accept(b);
          final ListObjectsV2Request request = b.build();
          requestAssertions.accept(request);
          return mockResponse;
        });
  }


  @Test
  public void testGetResourceAsBytes() {
    verifyGetObjectRequestBuilder(getObjectRequest -> {
          assertThat(getObjectRequest.bucket()).isEqualTo("test-bucket");
          assertThat(getObjectRequest.key())
              .isEqualTo("graphql-gateway/e2e/registrations/1.0.0/test-registration/config.json");
        },
        completedFuture(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[1]))
    );

    StepVerifier.create(
        clientUnderTest.getResourceAsByteArray("graphql-gateway/e2e/registrations/1.0.0/test-registration/config.json"))
        .consumeNextWith(contents -> assertThat(contents.length).isEqualTo(1))
        .verifyComplete();
  }

  private void verifyGetObjectRequestBuilder(Consumer<GetObjectRequest> requestAssertions,
      CompletableFuture<ResponseBytes<GetObjectResponse>> mockedResponse
  ) {
    when(asyncClient.getObject(any(Consumer.class), any(AsyncResponseTransformer.class)))
        .thenAnswer(invocation -> {
          Consumer<GetObjectRequest.Builder> consumer = invocation.getArgument(0);
          final Builder b = GetObjectRequest.builder();
          consumer.accept(b);
          final GetObjectRequest req = b.build();
          requestAssertions.accept(req);
          return mockedResponse;
        });
  }

}
