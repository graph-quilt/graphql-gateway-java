package com.intuit.graphql.gateway.s3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intuit.graphql.gateway.Mapper;
import com.intuit.graphql.gateway.Predicates;
import com.intuit.graphql.gateway.s3.RuleConfig.RuleType;
import io.vavr.control.Either;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class S3RuleEntryProcessor {

  static final String ERROR_NO_ENTRIES = "No valid file entries.";
  static final String ERROR_NO_CONFIG = "No valid 'config.yml' file found.";
  static final String ERROR_INVALID_CONFIG = "Failed to read config file.";
  static final String ERROR_NO_QUERIES = "No valid queries found.";
  static final String ERROR_FIELD_IS_NULL_TEMPLATE = "Field '%s' is missing from config.yml";

  /**
   * Processes an uncompressed ZIP file that contains AuthZ rules. Ignores zip file entries
   * where content is length 0 (i.e. the file entry is a directory instead of a .graphql or .yml file).
   *
   * @param zipFileEntries A Collection of uncompressed zip file entries.
   * @return A Processor Result that contains processed {@link S3Rule} or a list of errors that indicate an error in
   * processing.
   */
  public ProcessorResult processEntries(Collection<FileEntry> zipFileEntries) {
    List<FileEntry> fileEntries = cleanupFileEntries(zipFileEntries);
    if (fileEntries.isEmpty()) {
      AuthZProcessingError error = AuthZProcessingError.builder()
          .message(ERROR_NO_ENTRIES)
          .build();
      return ProcessorResult.builder()
          .queriesByClient(Collections.emptyMap())
          .errors(Collections.singletonList(error)).build();
    }

    Multimap<String, FileEntry> filesGroupedById = groupByFolder(fileEntries);

    return processInternal(filesGroupedById);
  }

  private List<FileEntry> cleanupFileEntries(Collection<FileEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      return Collections.emptyList();
    }
    return entries.stream()
        .filter(fileEntry -> fileEntry.contentInBytes().length > 0)
        .collect(Collectors.toList());
  }

  private ProcessorResult processInternal(Multimap<String, FileEntry> fileEntriesByFolder) {
    Map<String, S3Rule> results = new HashMap<>(fileEntriesByFolder.size());
    List<AuthZProcessingError> errors = new ArrayList<>();
    for (Entry<String, Collection<FileEntry>> entry : fileEntriesByFolder.asMap().entrySet()) {
      String id = entry.getKey();
      Collection<FileEntry> fileEntries = entry.getValue();

      Either<AuthZProcessingError, S3Rule> result = processEntry(id, fileEntries);
      if (result.isLeft()) {
        errors.add(result.getLeft());
      } else {
        results.put(id, result.get());
      }
    }

    return ProcessorResult.builder()
        .queriesByClient(results)
        .errors(errors)
        .build();
  }

  private Either<AuthZProcessingError, S3Rule> processEntry(String id, Collection<FileEntry> entries) {
    RuleConfig ruleConfig = null;
    List<String> queries = new ArrayList<>();
    String ruleBase = null;
    for (final FileEntry entry : entries) {
      if (isConfigFile(entry)) {
        try {
          ruleConfig = Mapper.yamlMapper().readValue(entry.contentInBytes(), RuleConfig.class);
        } catch (IOException e) {
          return Either.left(AuthZProcessingError.builder()
              .id(id)
              .fileEntry(entry)
              .message(ERROR_INVALID_CONFIG)
              .exception(e)
              .build());
        }
      } else if (isGraphQLFile(entry)) {
        queries.add(new String(entry.contentInBytes()));
      } else if (isRuleFile(entry)) {
        ruleBase = new String(entry.contentInBytes());
      }
      //ignore all other files
    }

    //Todo: Move all the validation to a custom lombok builder, then
    // throw and catch exception. // Use the data from exception for
    // eitherOR logic
    if (ruleConfig == null) {
      //no config file found
      return Either.left(AuthZProcessingError.builder()
          .id(id)
          .message(ERROR_NO_CONFIG)
          .build());
    } else {

      //these are better suited as mapper configurations on the class itself
      if (ruleConfig.getId() == null) {
        return Either.left(AuthZProcessingError.builder()
            .id(id)
            .message(String.format(ERROR_FIELD_IS_NULL_TEMPLATE, "id")).build()
        );
      }

      if (ruleConfig.getType() == null) {
        return Either.left(AuthZProcessingError.builder()
            .id(id)
            .message(String.format(ERROR_FIELD_IS_NULL_TEMPLATE, "type")).build());
      }
    }

    if (ruleConfig.getType() == RuleType.OFFLINE && queries.isEmpty()) {
      return Either.left(AuthZProcessingError.builder()
          .id(id)
          .message(ERROR_NO_QUERIES)
          .build());
    }

    final S3Rule s3Rule = S3Rule.builder()
        .ruleConfig(ruleConfig)
        .queries(queries)
        .rulebase(ruleBase)
        .build();

    return Either.right(s3Rule);

  }

  private Multimap<String, FileEntry> groupByFolder(Collection<FileEntry> fileEntries) {
    Multimap<String, FileEntry> results = HashMultimap.create();

    for (final FileEntry fileEntry : fileEntries) {
      /*
      Example absolute paths:
      graphql-authz-rules-1.0.0-SNAPSHOT/appid/query.graphql
      graphql-authz-rules-1.0.0-SNAPSHOT/appid/config.yml

      zip filename is always first entry, second is always folder that separates rules by an id.
     */
      String key = fileEntry.filename().split("/")[1];
      results.put(key, fileEntry);
    }

    return results;
  }

  private boolean isConfigFile(FileEntry fileEntry) {
    return fileEntry.filename().endsWith("config.yml");
  }


  private boolean isGraphQLFile(FileEntry fileEntry) {
    return Predicates.isSDLFile.test(fileEntry.filename());
  }

  private boolean isRuleFile(FileEntry fileEntry) {
    return Predicates.isRuleFile.test(fileEntry.filename());
  }

  @Getter
  @Builder
  public static class ProcessorResult {

    private final Map<String, S3Rule> queriesByClient;
    private final List<AuthZProcessingError> errors;
  }

  @Builder
  @Getter
  public static class AuthZProcessingError {

    @Nullable
    private final String id;

    @Nullable
    private final FileEntry fileEntry;

    @NonNull
    private final String message;

    @Nullable
    private final Exception exception;
  }
}
