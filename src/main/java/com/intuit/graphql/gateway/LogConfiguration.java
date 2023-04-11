package com.intuit.graphql.gateway;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties("log")
@Configuration
public class LogConfiguration {

  private boolean debug = false;

  @PostConstruct
  public void configureLogger() {
    Logger loggerImpl = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    if (debug) {
      loggerImpl.warn("Setting log level to " + Level.DEBUG.toString());
      loggerImpl.setLevel(Level.DEBUG);
    }
  }
}
