package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.config.properties.StitchingProperties;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import com.intuit.graphql.gateway.webclient.TxProvider;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


/**
 * Listens for property change events and re-builds the Data API graph when a relevant property changes.
 */
@Slf4j
@Component
public class SchemaPropertiesListener implements ApplicationListener<EnvironmentChangeEvent> {

  private final SchemaManager schemaManager;
  private final TxProvider txProvider;
  private final StitchingProperties properties;


  public SchemaPropertiesListener(SchemaManager schemaManager,
      TxProvider txProvider, StitchingProperties properties) {
    this.schemaManager = schemaManager;
    this.txProvider = txProvider;
    this.properties = properties;
  }

  @Override
  public void onApplicationEvent(EnvironmentChangeEvent event) {
    if (event.getKeys().stream().anyMatch(this::isStitchingConfigChanged)) {
      TransactionContext tx = txProvider
          .newTx("REBUILD-" + UUID.randomUUID().toString());
      EventLogger.info(log, tx, "stitching config changed, rebuilding schema",
          ImmutableLogNameValuePair.of(StitchingProperties.CONFIG_REBUILD, properties.isRebuild()));
      schemaManager.rebuildGraph(tx);
    }
  }

  /**
   * Test if a configuration key is relevant for rebuilding schema
   *
   * @param key The CaaS key that was updated
   * @return True if the key is relevant to for rebuilding False otherwise
   */
  private boolean isStitchingConfigChanged(final String key) {
    // Check if any of the stitching keys that require rebuild were updated
    // we don't want to update on other configuration changes
    return key != null && (key.startsWith(StitchingProperties.CONFIG_PREFIX));
  }
}

