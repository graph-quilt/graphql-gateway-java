package com.intuit.graphql.gateway.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.intuit.graphql.gateway.TestHelper;
import com.intuit.graphql.gateway.config.properties.AuthZProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

public class S3RuleRegistryTest {

  private S3RuleRegistry registryUnderTest;

  @Mock
  public ApplicationEventPublisher eventPublisher;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.registryUnderTest = new S3RuleRegistry(new TestPoller(), TestHelper.testTxProvider(), new S3Configuration(),
        eventPublisher, new AuthZProperties());
  }

  @Test
  public void testIsEmpty() {
    assertThat(this.registryUnderTest.isEmpty()).isTrue();
    this.registryUnderTest.cache(null, mock(S3RulePackage.class));
    assertThat(this.registryUnderTest.isEmpty()).isFalse();
  }

  @Test
  public void testUpdate() {
    this.registryUnderTest.cache(null, mock(S3RulePackage.class));
    this.registryUnderTest.update().block();

    verify(eventPublisher, times(1)).publishEvent(any());
  }

  static class TestPoller implements S3Poller<S3RulePackage> {

    @Override
    public Mono<Void> buildPollingSequence(final S3Registry<S3RulePackage> registry) {
      return Mono.empty();
    }


  }
}
