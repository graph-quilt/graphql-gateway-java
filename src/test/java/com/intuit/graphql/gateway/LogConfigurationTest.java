package com.intuit.graphql.gateway;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class LogConfigurationTest {

  @Test
  public void testLogLevelOnDebugFalse() {
    LogConfiguration logConfiguration = new LogConfiguration();
    logConfiguration.setDebug(false);
    logConfiguration.configureLogger();
    Logger loggerImpl = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    Assert.assertEquals(Level.INFO, loggerImpl.getLevel());
  }

  @Test
  public void testLogLevelOnDebugTrue() {
    LogConfiguration logConfiguration = new LogConfiguration();
    logConfiguration.setDebug(true);
    logConfiguration.configureLogger();
    Logger loggerImpl = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    Assert.assertEquals(Level.DEBUG, loggerImpl.getLevel());
  }
}
