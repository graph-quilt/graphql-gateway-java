package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.config.properties.AuthZProperties;
import com.intuit.graphql.gateway.Mapper;
import io.vavr.control.Try;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides methods that transform S3 Response objects into AuthZ-related objects.
 */
@Component
public class AuthZS3Client {

  private final ReactorS3Client reactorS3Client;
  private final AuthZProperties authzProperties;

  public AuthZS3Client(final ReactorS3Client reactorS3Client,
      final AuthZProperties authzProperties) {
    this.reactorS3Client = reactorS3Client;
    this.authzProperties = authzProperties;
  }

  public Mono<S3RuleVersionsFile> getAuthZVersionFile() {
    final String s3Path = reactorS3Client.s3ApplicationPrefix()
        .add(authzProperties.getRulesDirectory())
        .add(authzProperties.getVersionsFileName())
        .toString();

    return reactorS3Client.getResourceAsByteArray(s3Path)
        .flatMap(content -> Try.of(() -> Mapper.yamlMapper().readValue(content, S3RuleVersionsFile.class))
            .map(Mono::just)
            .getOrElseGet(Mono::error));
  }

  public Flux<FileEntry> downloadAuthZRulesZip(final String s3Path) {
    return reactorS3Client.getResourceAsByteArray(s3Path)
        .flatMapMany(zipContents -> Flux.fromIterable(ZipUtil.uncompress(zipContents)));
  }
}
