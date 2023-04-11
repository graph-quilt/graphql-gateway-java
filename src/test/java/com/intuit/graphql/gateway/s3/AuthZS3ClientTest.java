package com.intuit.graphql.gateway.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.gateway.config.properties.AuthZProperties;
import java.util.List;
import java.util.StringJoiner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;

public class AuthZS3ClientTest {

  private AuthZS3Client clientUnderTest;

  @Mock
  public ReactorS3Client reactorS3Client;

  AuthZProperties properties;

  @Before
  public void setUp() {
    initMocks(this);
    properties = new AuthZProperties();
    clientUnderTest = new AuthZS3Client(reactorS3Client, properties);
  }

  @Test
  public void testGetAuthZVersionsFile() {
    doReturn(new StringJoiner("/").add("graphql-gateway")).when(reactorS3Client).s3ApplicationPrefix();
    doReturn(Mono.just(
        TestHelper.read("mocks.authz-rules/authz-s3-config.yml")
    )).when(reactorS3Client).getResourceAsByteArray(eq("graphql-gateway/authz-rules/versions.yml"));

    final S3RuleVersionsFile result = clientUnderTest.getAuthZVersionFile().block();

    assertThat(result.getLatest()).isEqualTo("test");
  }

  @Test
  public void testDownloadZipFile() {
    doReturn(Mono.just(TestHelper.read("mocks.authz-rules/test-rules.zip"))).when(reactorS3Client)
        .getResourceAsByteArray(eq("test"));

    final List<FileEntry> result = clientUnderTest.downloadAuthZRulesZip("test").collectList().block();

    assertThat(result).isNotEmpty();
  }
}
