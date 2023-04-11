package com.intuit.graphql.gateway.s3;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.intuit.graphql.gateway.LoggingListenerRule;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.s3.S3RuleEntryProcessor.AuthZProcessingError;
import com.intuit.graphql.gateway.s3.S3RuleEntryProcessor.ProcessorResult;
import com.intuit.graphql.gateway.webclient.TxProvider;
import com.intuit.graphql.gateway.TestHelper;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class S3RulePollerTest {

  @Rule
  public LoggingListenerRule loggerRule = new LoggingListenerRule();

  @Mock
  public AuthZS3Client client;

  @Mock
  public S3RuleEntryProcessor processor;

  @Mock
  public TxProvider txProvider;

  @Mock
  public S3RuleRegistry provider;

  /*
  zip contents:

  ```
  /test-rules
    /test
      /config.yml
      /query.graphql
  ```

  config.yml contents:

  ```
  id: test
  description: test
  type: OFFLINE
  ```

  query.graphql contents:

  ```
  { foo }
  ```
   */
  public byte[] zipFile = TestHelper.read("mocks.authz-rules/test-rules.zip");

  public S3RulePoller pollerUnderTest;

  @Before
  public void setUp() {
    initMocks(this);
    Mockito.doReturn(TestHelper.testTx()).when(txProvider).newTx(anyString());
    Mockito.doReturn(TestHelper.testTx()).when(txProvider).newTx();
    when(provider.update()).thenReturn(Mono.empty());
  }

  private void initCache() {
    S3RuleVersionsFile version = new S3RuleVersionsFile();
    version.setLatest("test-latest");
    doReturn(Mono.just(version)).when(client).getAuthZVersionFile();
    doReturn(Flux.fromIterable(ZipUtil.uncompress(zipFile))).when(client).downloadAuthZRulesZip(eq("test-latest"));
    Map<String, S3Rule> expectedMap = Collections.emptyMap();

    ProcessorResult result = ProcessorResult.builder().errors(Collections.emptyList()).queriesByClient(expectedMap)
        .build();

    doReturn(result).when(processor).processEntries(anyList());

    pollerUnderTest = new S3RulePoller(client, txProvider, processor);
    pollerUnderTest.fetch(provider, TestHelper.testTx()).block();
  }

  @Test
  public void testFetch() {
    initCache();
    verify(provider).cache(any(TransactionContext.class), any(S3RulePackage.class));
    verify(provider, never()).update();
  }

  @Test
  public void cacheIsNotCalledIfAuthZFilesNotChanged() {
    S3RuleVersionsFile version = new S3RuleVersionsFile();
    version.setLatest("test-latest");

    when(provider.isEmpty()).thenReturn(false);
    doReturn(Mono.just(version)).when(client).getAuthZVersionFile();

    pollerUnderTest = new S3RulePoller(client, txProvider, processor);

    pollerUnderTest.authzVersionsFile = version;

    pollerUnderTest.buildPollingSequence(provider).block();

    verify(provider, never()).cache(any(TransactionContext.class), any(S3RulePackage.class));
    verify(provider, never()).update();
  }

  @Test
  public void logsParsingErrorWithException() {
    pollerUnderTest = new S3RulePoller(client, txProvider, processor);

    AuthZProcessingError error = AuthZProcessingError.builder()
        .id("test-id")
        .message("Invalid test file.")
        .exception(new RuntimeException("boom"))
        .fileEntry(ImmutableFileEntry.builder()
            .contentInBytes()
            .filename("test-file-name")
            .build()).build();

    pollerUnderTest.logProcessingErrors(null, Collections.singletonList(error));

    loggerRule.assertLogsContain("errorMessage", "Invalid test file.");
    loggerRule.assertLogsContain("id", "test-id");
    loggerRule.assertLogsContain("fileName", "test-file-name");
    loggerRule.assertLogsContainKey("nestedExceptions");
  }

  @Test
  public void logsParingErrorsWithoutException() {
    pollerUnderTest = new S3RulePoller(client, txProvider, processor);

    AuthZProcessingError error = AuthZProcessingError.builder()
        .id("test-id")
        .message("No config file found")
        .build();

    pollerUnderTest.logProcessingErrors(null, Collections.singletonList(error));

    loggerRule.assertLogsContain("errorMessage", "No config file found");
    loggerRule.assertLogsContain("id", "test-id");
  }
}
