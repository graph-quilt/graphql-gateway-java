package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.config.properties.AuthZProperties;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.webclient.TxProvider;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class S3RuleRegistry implements S3Registry<S3RulePackage> {

  private final S3Poller<S3RulePackage> poller;
  private final TxProvider txProvider;
  private final S3Configuration s3Configuration;
  private final ApplicationEventPublisher eventPublisher;
  private final AuthZProperties authZProperties;

  private volatile S3RulePackage cachedResource = null;

  public S3RuleRegistry(S3Poller<S3RulePackage> poller, TxProvider txProvider, S3Configuration s3Configuration,
      ApplicationEventPublisher eventPublisher, final AuthZProperties authZProperties) {
    this.poller = poller;
    this.txProvider = txProvider;
    this.s3Configuration = s3Configuration;
    this.eventPublisher = eventPublisher;
    this.authZProperties = authZProperties;
  }


  @PostConstruct
  public void configurePolling() {
    if (!authZProperties.isEnabled()) {
      log.warn("AuthZ is disabled!");
      return;
    }

    poller.fetch(this, txProvider.newTx("INIT-RULES-" + UUID.randomUUID().toString())).block();

    if (s3Configuration.getPolling().isEnabled()) {
      Mono.just(poller)
          .doOnNext(poll -> Mono.defer(() -> poll.buildPollingSequence(this)
              .delaySubscription(s3Configuration.getPolling().getPeriod(), poll.getDefaultScheduler()))
              .onErrorResume(e -> Mono.empty())
              .repeat(() -> s3Configuration.getPolling().isEnabled())
              .subscribe()
          ).doOnError(e -> EventLogger
          .error(log, txProvider.newTx(), "Failed to start polling sequence for rules.", e))
          .subscribe();
    }
  }

  @Override
  public boolean isEmpty() {
    return cachedResource == null;
  }

  @Override
  public Mono<Void> update() {
    return Mono.fromRunnable(() -> eventPublisher.publishEvent(S3RulesChangedEvent.INSTANCE));
  }

  @Override
  public S3RulePackage get() {
    return this.cachedResource;
  }

  @Override
  public S3RulePackage cache(final TransactionContext tx, final S3RulePackage resource) {
    this.cachedResource = resource;
    return resource;
  }

  public static class S3RulesChangedEvent extends ApplicationEvent {

    public static final S3RulesChangedEvent INSTANCE = new S3RulesChangedEvent(new Object());

    public S3RulesChangedEvent(final Object source) {
      super(source);
    }
  }
}
