package com.intuit.graphql.gateway.logging;

import static net.logstash.logback.marker.Markers.append;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableLogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Marker;

public class MarkerUtil {

  private static final String PHASE_NAME = "phase";
  private static final String REQUEST_PAYLOAD_NAME = "requestPayload";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Helper Method to build out the Log Data to be logged
   *
   * @param phase event phase
   * @param transContext the transaction context
   * @param subContext the external context
   * @param nvPairs additional name value pairs to be logged
   * @return data to be logged
   */
  public static Marker generateMarker(String phase, TransactionContext transContext,
      Marker subContext, LogNameValuePair... nvPairs) {
    return createMarkerForTransaction(phase, transContext, subContext, nvPairs);
  }

  private static Marker createMarkerForTransaction(String phase, TransactionContext transContext,
      Marker subContext, LogNameValuePair... nvPairs) {

    Marker logData = append(PHASE_NAME, phase);

    if (transContext != null) {
      ObjectNode objectNode = objectMapper.valueToTree(transContext);
      objectNode.fields().forEachRemaining(x -> logData.add(append(x.getKey(), x.getValue())));
    }

    if (subContext != null) {
      logData.add(subContext);
    }

    if (nvPairs != null) {
      for (LogNameValuePair nvPair : removeAnyNullEntries(nvPairs)) {
        logData.add(append(nvPair.getName(), nvPair.getValue()));
      }
    }
    return logData;
  }

  public static Marker addPayloadData(JsonNode jsonPayload, String payload) {
    if (jsonPayload != null) {
      return append(REQUEST_PAYLOAD_NAME, jsonPayload);
    } else if (payload != null) {
      return append(REQUEST_PAYLOAD_NAME, payload);
    }
    return null;
  }

  /**
   * Filter out any null parameters in the vararg to avoid an NPE when using the args.
   *
   * @param nvPairs the vararg to process
   * @return new <code>LogNameValuePair</code> object array correctly sized with no null entries
   */
  static LogNameValuePair[] removeAnyNullEntries(LogNameValuePair... nvPairs) {
    return Arrays
        .stream(nvPairs).filter(Objects::nonNull)
        .toArray(ImmutableLogNameValuePair[]::new);
  }
}
