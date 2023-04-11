package com.intuit.graphql.gateway.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.intuit.graphql.gateway.config.properties.StitchingProperties;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.webclient.TxProvider;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import reactor.core.publisher.Mono;

public class SchemaPropertiesListenerTest {

  @Mock
  SchemaManager schemaManager;

  @Mock
  TxProvider txProvider;

  TransactionContext tx = ImmutableTransactionContext.builder().build();

  @Mock
  StitchingProperties properties;

  public SchemaPropertiesListenerTest() {
    MockitoAnnotations.initMocks(this);
    setupTransactionContext();
  }

  private void setupTransactionContext() {
    when(txProvider.newTx(anyString())).thenReturn(tx);
  }

  @Test
  public void onApplicationEventRebuildsIfRelevantTest() {
    when(schemaManager.updateRegistry(any(), any())).thenReturn(Mono.empty());

    SchemaPropertiesListener listener = new SchemaPropertiesListener(schemaManager,
        txProvider,
        properties);

    verify(schemaManager, times(0)).rebuildGraph(any());

    EnvironmentChangeEvent event = new EnvironmentChangeEvent(new Object(),
        Collections.singleton(StitchingProperties.CONFIG_PREFIX));
    listener.onApplicationEvent(event);
    verify(schemaManager, times(1)).rebuildGraph(any());
  }

  @Test
  public void onApplicationEventDoesNotRebuildIfNotRelevantTest() {
    when(schemaManager.updateRegistry(any(), any())).thenReturn(Mono.empty());

    SchemaPropertiesListener listener = new SchemaPropertiesListener(schemaManager,
        txProvider,
        properties);

    verify(schemaManager, times(0)).rebuildGraph(any());

    EnvironmentChangeEvent event = new EnvironmentChangeEvent(new Object(), Collections.singleton("some other key"));
    listener.onApplicationEvent(event);
    verify(schemaManager, times(0)).rebuildGraph(any());
  }

  @Test
  public void nullKeysDontFail() {
    when(schemaManager.updateRegistry(anyString(), any())).thenReturn(Mono.empty());

    SchemaPropertiesListener listener = new SchemaPropertiesListener(schemaManager,
        txProvider, properties);

    EnvironmentChangeEvent event = new EnvironmentChangeEvent(new Object(),
        Collections.singleton(null));
    listener.onApplicationEvent(event);
    verify(schemaManager, never()).rebuildGraph(any());
  }
}
