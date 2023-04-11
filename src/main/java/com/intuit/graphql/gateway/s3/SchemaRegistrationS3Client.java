package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.s3.RegistrationPoller.RegistrationResource;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.S3Object;

@Configuration
@Slf4j
public class SchemaRegistrationS3Client {

  private static final String REGISTRATION_PATH_PREFIX = "registrations";
  private final ReactorS3Client reactorS3Client;
  private final S3Configuration s3Configuration;

  public SchemaRegistrationS3Client(ReactorS3Client reactorS3Client, final S3Configuration s3Configuration) {
    this.reactorS3Client = reactorS3Client;
    this.s3Configuration = s3Configuration;
  }

  /**
   * List all registrations for the path prefix build from {@link S3Configuration}. Only main resources are filtered to
   * included in list of registrations.
   *
   * @return a Flux that emits all registration S3Objects found in S3
   */
  public Flux<S3Object> listRegistrations() {

    final String registrationPrefix = reactorS3Client.s3ApplicationPrefix()
        .add(REGISTRATION_PATH_PREFIX)
        .add(s3Configuration.getVersion())
        .toString();

    Predicate<String> isMainResourcePredicate = Predicates
        .isMainResourcePredicate(registrationPrefix);

    return reactorS3Client.listObjects(registrationPrefix)
        .flatMapMany(r -> Flux.fromIterable(r.contents()))
        .filter(s3Object -> isMainResourcePredicate.test(s3Object.key())) // filter main resources only
        .filter(s3Object -> Predicates.isRegistrationFile.test(s3Object.key()));
  }

  public Mono<RegistrationResource> downloadRegistrationResource(S3Object s3Object) {
    return reactorS3Client.getResourceAsByteArray(s3Object.key())
        .map(bytes -> RegistrationResource.builder().s3Object(s3Object).content(bytes).build());
  }
}
