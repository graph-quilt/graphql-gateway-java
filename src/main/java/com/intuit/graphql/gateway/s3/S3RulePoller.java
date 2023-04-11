package com.intuit.graphql.gateway.s3;

import com.google.common.annotations.VisibleForTesting;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.s3.S3RuleEntryProcessor.AuthZProcessingError;
import com.intuit.graphql.gateway.webclient.TxProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class S3RulePoller implements S3Poller<S3RulePackage> {

  private final AuthZS3Client authZS3Client;
  private final TxProvider txProvider;
  private final S3RuleEntryProcessor processor;

  @VisibleForTesting
  S3RuleVersionsFile authzVersionsFile = null;

  public S3RulePoller(final AuthZS3Client authZS3Client, TxProvider txProvider,
      S3RuleEntryProcessor processor) {
    this.authZS3Client = authZS3Client;
    this.txProvider = txProvider;
    this.processor = processor;
  }

  @Override
  public Mono<Void> buildPollingSequence(final S3Registry<S3RulePackage> registry) {
    TransactionContext tx = txProvider.newTx("RULES-" + UUID.randomUUID().toString());
    return Mono.defer(authZS3Client::getAuthZVersionFile)
        .flatMap(s3RuleVersionsFile -> doesRegistryNeedUpdate(s3RuleVersionsFile, registry)
            ? downloadNewRules(registry, tx, s3RuleVersionsFile)
            .then(Mono.defer(registry::update))
            : logAndSkip(tx));
  }

  @Override
  public Mono<Void> fetch(final S3Registry<S3RulePackage> registry, final TransactionContext tx) {
    return Mono.defer(authZS3Client::getAuthZVersionFile)
        .flatMap(s3RuleVersionsFile -> doesRegistryNeedUpdate(s3RuleVersionsFile, registry)
            //fetch does not update registry.
            ? downloadNewRules(registry, tx, s3RuleVersionsFile) : logAndSkip(tx));
  }

  private Mono<Void> downloadNewRules(final S3Registry<S3RulePackage> registry, final TransactionContext tx,
      S3RuleVersionsFile versionsFile) {
    return logThenFetchRules(tx, versionsFile)
        .doOnNext(authZFiles -> registry.cache(tx, authZFiles))
        .doOnSuccess(notUsed -> this.authzVersionsFile = versionsFile)
        .doOnSuccess(notUsed -> logLatestVersion(tx, versionsFile))
        .then();
  }

  private void logLatestVersion(TransactionContext tx, S3RuleVersionsFile versionsFile) {
    final LogNameValuePair version = ImmutableLogNameValuePair
        .of("version", versionsFile != null ? versionsFile.getLatest() : null);
    EventLogger.info(log, tx, "Successfully installed rules", version);
  }

  private Mono<Void> logAndSkip(TransactionContext tx) {
    return Mono.fromRunnable(() -> {
      final LogNameValuePair version = ImmutableLogNameValuePair
          .of("version", authzVersionsFile != null ? authzVersionsFile.getLatest() : null);

      EventLogger.info(log, tx, "Skipping AuthZ version", version);
    }).then();
  }

  private Mono<S3RulePackage> logThenFetchRules(final TransactionContext tx, final S3RuleVersionsFile newVersionFile) {
    return Mono.fromRunnable(() -> {
      final LogNameValuePair oldVersion = ImmutableLogNameValuePair
          .of("oldVersion", authzVersionsFile != null ? authzVersionsFile.getLatest() : null);
      final LogNameValuePair newVersion = ImmutableLogNameValuePair.of("newVersion", newVersionFile.getLatest());

      EventLogger.info(log, tx, "Found new AuthZ version", oldVersion, newVersion);
    })
        .then(Mono.defer(() -> this.fetchRules(tx, newVersionFile)));
  }

  private Mono<S3RulePackage> fetchRules(final TransactionContext tx, final S3RuleVersionsFile authzVersionsFile) {
    return authZS3Client.downloadAuthZRulesZip(authzVersionsFile.getLatest())
        .filter(fileEntry -> fileEntry.contentInBytes().length > 0) //folders have no content length
        .collectList()
        .filter(entries -> !entries.isEmpty())
        .doOnNext(fileEntries -> {
          List<String> filenames = fileEntries.stream().map(FileEntry::filename).collect(Collectors.toList());
          EventLogger.info(log, tx, "Found authZ entries",
              ImmutableLogNameValuePair.of("entries", filenames));
        })
        .map(processor::processEntries)
        .doOnNext(processorResult -> {
          if (!processorResult.getErrors().isEmpty()) {
            logProcessingErrors(tx, processorResult.getErrors());
          }
        })
        .map(processorResult -> S3RulePackage.builder().authzRulesById(processorResult.getQueriesByClient()).build());
  }

  private boolean doesRegistryNeedUpdate(S3RuleVersionsFile authzVersionsFile, S3Registry<?> registry) {
    //registry that is empty means not initialized yet
    //null authZ version file means poller has not initialized yet
    //different authz version files means there is an update
    return registry.isEmpty() ||
        this.authzVersionsFile == null ||
        !this.authzVersionsFile.getLatest().equals(authzVersionsFile.getLatest());
  }

  void logProcessingErrors(TransactionContext tx, List<AuthZProcessingError> errors) {
    for (final AuthZProcessingError error : errors) {
      List<LogNameValuePair> pairs = new ArrayList<>(3);

      pairs.add(ImmutableLogNameValuePair.of("id", error.getId()));
      pairs.add(ImmutableLogNameValuePair.of("errorMessage", error.getMessage()));

      if (error.getFileEntry() != null) {
        pairs.add(ImmutableLogNameValuePair.of("fileName", error.getFileEntry().filename()));
      }

      if (error.getException() != null) {
        EventLogger.warn(log, tx, "Error in AuthZ rules", error.getException(), pairs.toArray(new LogNameValuePair[3]));
      } else {
        EventLogger.warn(log, tx, "Error in AuthZ rules", pairs.toArray(new LogNameValuePair[3]));
      }
    }
  }

}
