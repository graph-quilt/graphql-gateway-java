package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import java.util.StringJoiner;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Provides methods that convert common async S3 API methods to return Reactor Mono/Flux. Bucket and application
 * configuration are automatically allocated to requests using {@code S3Configuration}. Also provides retry
 * functionality for all S3 API calls.
 */
@Component
@Slf4j
public class ReactorS3Client {

  private final S3AsyncClient s3AsyncClient;
  private final S3Configuration s3Configuration;

  public ReactorS3Client(final S3AsyncClient s3AsyncClient, final S3Configuration s3Configuration) {
    this.s3AsyncClient = s3AsyncClient;
    this.s3Configuration = s3Configuration;
  }

  /**
   * Creates a StringJoiner with application prefix and environment to construct path to required S3 resource.
   * <p>
   * For example, a {@code S3Configuration#getAppName()} that produces {@code graphql-gateway} and a {@code
   * S3Configuration#getEnv()} that produces {@code e2e} will produce a StringJoiner with the contents {@code
   * graphql-gateway/e2e/}
   *
   * @return A StringJoiner to build path sequence using fileSeparator as a delimiter.
   */
  public StringJoiner s3ApplicationPrefix() {
    return new StringJoiner("/")
        .add(s3Configuration.getAppName())
        .add(s3Configuration.getEnv());
  }

  /**
   * Issues an S3 {@code GetObjectRequest} with the bucket configured to {@code S3Configuration#getBucket()}.
   *
   * @param key the absolute s3 path to the file
   * @return A Mono that emits the file contents (as bytes).
   */
  public Mono<ResponseBytes<GetObjectResponse>> getObject(final String key) {
    return Mono.fromFuture(() ->
        s3AsyncClient.getObject(request -> request.bucket(s3Configuration.getPolling().getBucketname()).key(key),
            AsyncResponseTransformer.toBytes()))
        .retryWhen(Retry.withThrowable(this.retryFactory(key)))
        .doOnError(
            err -> EventLogger.error(log, null, "Failed to get object", err, ImmutableLogNameValuePair
                .of("objectKey", key)));
  }

  /**
   * Issues an S3 {@code ListObjectsRequest} with the bucket configured to {@code S3Configuration#getBucket()}.
   *
   * @param prefix a '/' delimited prefix
   * @return A Mono that emits the S3 response
   */
  public Mono<ListObjectsV2Response> listObjects(final String prefix) {
    return Mono.fromFuture(() ->
        s3AsyncClient
            .listObjectsV2(request -> request.bucket(s3Configuration.getPolling().getBucketname()).prefix(prefix)))
        .retryWhen(Retry.withThrowable(this.retryFactory("listObjectsRequest")))
        .doOnError(err -> EventLogger
            .error(log, null, "Failed to list objects", err, ImmutableLogNameValuePair.of("listPrefix", prefix)));
  }

  /**
   * Issues an S3 {@code GetObjectRequest} for any file using the absolute path to the resource.
   *
   * @param fullS3Path contains the absolute path of the requested resource.
   * @return A mono that emits the contents of the file (as String)
   */
  public Mono<byte[]> getResourceAsByteArray(final String fullS3Path) {
    return getObject(fullS3Path)
        .map(ResponseBytes::asByteArray)
        .retryWhen(Retry.withThrowable(this.retryFactory(fullS3Path)))
        .flatMap(Mono::justOrEmpty);
  }

  private Function<Flux<Throwable>, ? extends Publisher<?>> retryFactory(String key) {
    return eFlux -> eFlux
        .zipWith(Flux.range(1, s3Configuration.getPolling().getMaxRetryAttempts() + 1), (e, attempts) -> {
          if (e instanceof S3Exception) {
            S3Exception s3Exception = (S3Exception) e;
            if (s3Exception.statusCode() > 399 && s3Exception.statusCode() < 500) {
              log.error("Received non 5xx error from S3. Aborting.", e);
              throw Exceptions.propagate(e);
            }
          }

          if (attempts <= s3Configuration.getPolling().getMaxRetryAttempts()) {
            log.warn("Retrying S3 request. Key[" + key + "] Attempts[" + attempts + "]", e);
            return attempts;
          } else {
            log.error("Max attempts reached. Aborting.", e);
            throw Exceptions.propagate(e);
          }
        });
  }
}
