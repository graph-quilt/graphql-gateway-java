package com.intuit.graphql.gateway.webclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.config.properties.AppLoggingProperties;
import com.intuit.graphql.gateway.config.properties.AppSecurityProperties;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.GraphQLContext;
import java.util.Optional;
import org.junit.Test;
import reactor.util.context.Context;

public class TxProviderTest {

  @Test
  public void providerGivesDefaultTxWhenNoContextTest() {

    TransactionContext tx = TxProvider.embeddedTx().block();

    assertThat(tx.getTid()).startsWith("empty-");
  }

  @Test
  public void generatesNewTxWithApplicationContext() {
    final AppLoggingProperties appLoggingProperties = new AppLoggingProperties();
    final AppSecurityProperties appSecurityProperties = new AppSecurityProperties();

    appLoggingProperties.setAppId("test-app-id");
    appLoggingProperties.setVersion("test-version");
    appLoggingProperties.setEnv("test-env");

    appSecurityProperties.setAppId("test-offering-id");
    TxProvider txProvider = new TxProvider(appSecurityProperties, appLoggingProperties);

    final TransactionContext transactionContext = txProvider.newTx();

    assertThat(transactionContext.getAppId()).isEqualTo("test-app-id");
    assertThat(transactionContext.getVersion()).isEqualTo("test-version");
    assertThat(transactionContext.getEnv()).isEqualTo("test-env");
  }

  @Test
  public void getTxReturnsOptionalTransactionContextPresent() {
    TransactionContext transactionContextMock = mock(TransactionContext.class);
    Context reactorContextMock = mock(Context.class);
    when(reactorContextMock.getOrEmpty(TransactionContext.class)).thenReturn(Optional.of(transactionContextMock));
    GraphQLContext graphQLContextMock = mock(GraphQLContext.class);
    when(graphQLContextMock.getOrEmpty(Context.class)).thenReturn(Optional.of(reactorContextMock));
    Optional<TransactionContext> actual = TxProvider.getTx(graphQLContextMock);

    assertThat(actual.get()).isSameAs(transactionContextMock);
  }

  @Test
  public void getTxReturnsOptionalTransactionContextNotPresent() {
    Context unexpectedContext = mock(Context.class);
    Optional<TransactionContext> actual = TxProvider.getTx(unexpectedContext);
    assertThat(actual.isPresent()).isFalse();
  }
}
