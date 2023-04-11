package com.intuit.graphql.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.assertj.core.groups.Tuple;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

//todo if test fails assertion for logs, a class configuration error will be produced instead of failure. Should return assertion class.
@Getter
public class LoggingListenerRule implements TestRule {

  private TestAppender testAppender;
  private List<ObjectAppendingMarker> logFields;

  @SneakyThrows
  @Override
  public Statement apply(final Statement base, final Description description) {
    Logger logger = (Logger) LoggerFactory.getLogger("ROOT");
    testAppender = new TestAppender();
    logger.addAppender(testAppender);
    base.evaluate();
    logger.detachAppender(testAppender);
    return base;
  }

  public void assertLogsContain(String key, String value) {
    initLogCache();

    assertThat(logFields)
        .extracting(ObjectAppendingMarker::getFieldName, ObjectAppendingMarker::getFieldValue)
        .contains(Tuple.tuple(key, value));
  }

  public void assertLogsContainKey(String key) {
    initLogCache();

    assertThat(logFields)
        .extracting(ObjectAppendingMarker::getFieldName)
        .contains(key);
  }

  public void assertLogsContainValueSubstring(String value) {
    initLogCache();

    assertThat(logFields)
        .extracting(objectAppendingMarker -> (String) objectAppendingMarker.getFieldValue())
        .anyMatch(s -> s.contains(value));

  }

  private void initLogCache() {
    if (logFields == null) {
      logFields = new ArrayList<>();
      logFields.addAll(testAppender.logs.stream().flatMap(this::extractMarkers).collect(Collectors.toList()));
    }
  }

  private Stream<ObjectAppendingMarker> extractMarkers(ILoggingEvent loggingEvent) {
    //include root marker because it is also a log key/value pair
    final Stream<Marker> s1 = Stream.of(loggingEvent.getMarker());
    final Stream<Marker> s2 = Lists.newArrayList(loggingEvent.getMarker().iterator()).stream();

    return Stream.concat(s1, s2).map(marker -> (ObjectAppendingMarker) marker);
  }

  public static class TestAppender extends AppenderBase<ILoggingEvent> {

    List<ILoggingEvent> logs = new ArrayList<>();

    @Override
    public synchronized void doAppend(final ILoggingEvent eventObject) {
      logs.add(eventObject);
      //must call super to see logs in console
      super.doAppend(eventObject);
    }

    @Override
    protected void append(final ILoggingEvent event) {
      //do nothing, append is not called.
    }
  }
}
