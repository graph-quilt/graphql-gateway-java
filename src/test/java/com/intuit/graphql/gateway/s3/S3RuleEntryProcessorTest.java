package com.intuit.graphql.gateway.s3;

import static com.intuit.graphql.gateway.TestHelper.read;
import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.gateway.s3.S3RuleEntryProcessor.AuthZProcessingError;
import com.intuit.graphql.gateway.s3.S3RuleEntryProcessor.ProcessorResult;
import com.intuit.graphql.gateway.s3.RuleConfig.RuleType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class S3RuleEntryProcessorTest {

  S3RuleEntryProcessor processor = new S3RuleEntryProcessor();

  @Test
  public void testProcessorAgainstZipFile() {
    List<FileEntry> fileEntries = ZipUtil.uncompress(read("mocks.authz-rules/test-rules.zip"));

    final ProcessorResult result = processor.processEntries(fileEntries);
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getQueriesByClient().get("test").getRuleConfig().getId()).isEqualTo("test");
    assertThat(result.getQueriesByClient().get("test").getRuleConfig().getDescription()).isEqualTo("test");
    assertThat(result.getQueriesByClient().get("test").getRuleConfig().getType())
        .isEqualTo(RuleType.OFFLINE);
    assertThat(result.getQueriesByClient().get("test").getQueries().get(0))
        .contains("{ foo }");
  }

  @Test
  public void testProcessConfigFileWithSingleQuery() {
    FileEntry configEntry = ImmutableFileEntry.builder()
        .filename("zip/test/config.yml")
        .contentInBytes(read("mocks.authz-rules/config.yml"))
        .build();

    FileEntry queryEntry = ImmutableFileEntry.builder()
        .filename("zip/test/query.graphql")
        .contentInBytes("test".getBytes()).build();

    FileEntry txtEntry = ImmutableFileEntry.builder()
        .filename("zip/test/query.txt")
        .contentInBytes("test".getBytes()).build();

    final ProcessorResult result = processor.processEntries(Arrays.asList(configEntry, queryEntry));

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getQueriesByClient()).hasSize(1);
  }

  @Test
  public void testProcessNoConfig() {
    FileEntry queryEntry = ImmutableFileEntry.builder()
        .filename("zip/test/query.graphql")
        .contentInBytes("test".getBytes()).build();
    final ProcessorResult result = processor.processEntries(Collections.singletonList(queryEntry));

    assertThat(result.getErrors().get(0).getMessage()).isEqualTo(S3RuleEntryProcessor.ERROR_NO_CONFIG);
  }

  @Test
  public void testMissingIdConfig() {
    FileEntry queryEntry = ImmutableFileEntry.builder()
        .filename("zip/test/config.yml")
        .contentInBytes(read("mocks.authz-rules/missing-id.yml")).build();
    final ProcessorResult result = processor.processEntries(Collections.singletonList(queryEntry));

    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo(String.format(S3RuleEntryProcessor.ERROR_FIELD_IS_NULL_TEMPLATE,"id"));
  }

  @Test
  public void testMissingTypeConfig() {
    FileEntry queryEntry = ImmutableFileEntry.builder()
        .filename("zip/test/config.yml")
        .contentInBytes(read("mocks.authz-rules/missing-type.yml")).build();
    final ProcessorResult result = processor.processEntries(Collections.singletonList(queryEntry));

    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo(String.format(S3RuleEntryProcessor.ERROR_FIELD_IS_NULL_TEMPLATE,"type"));
  }

  @Test
  public void testInvalidConfigFile() {
    FileEntry configEntry = ImmutableFileEntry.builder()
        .filename("zip/test/config.yml")
        .contentInBytes("invalid".getBytes())
        .build();

    final ProcessorResult result = processor.processEntries(Collections.singletonList(configEntry));
    AuthZProcessingError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo(S3RuleEntryProcessor.ERROR_INVALID_CONFIG);
    assertThat(error.getId()).isEqualTo("test");
    assertThat(error.getException()).isNotNull();
    assertThat(error.getFileEntry()).isSameAs(configEntry);
  }

  @Test
  public void testNoEntries() {
    final ProcessorResult result = processor.processEntries(Collections.emptyList());

    final AuthZProcessingError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo(S3RuleEntryProcessor.ERROR_NO_ENTRIES);
  }

  @Test
  public void testProcessNoQueries() {
    FileEntry configEntry = ImmutableFileEntry.builder()
        .filename("zip/test/config.yml")
        .contentInBytes(read("mocks.authz-rules/config.yml"))
        .build();

    final ProcessorResult result = processor.processEntries(Collections.singletonList(configEntry));

    final AuthZProcessingError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo(S3RuleEntryProcessor.ERROR_NO_QUERIES);
    assertThat(error.getId()).isEqualTo("test");
  }
}
