package com.intuit.graphql.gateway.logging;

import static org.springframework.http.HttpHeaders.COOKIE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.graphql.gateway.logging.interfaces.LogNameValuePair;
import com.intuit.graphql.gateway.logging.interfaces.LoggingContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import net.logstash.logback.marker.Markers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class EventLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextFactory.class);
  public static final String TRANS_CONTEXT_TAG = "transContext";
  private static final String PHASE_NAME = "phase";
  private static final String SUB_CTX_NAME = "subtaskContext";
  private static final String PHASE_START = "start";
  private static final String PHASE_END = "end";
  private static final String PHASE_INTERMEDIATE = "intermediate";
  private static final String PHASE_SUB_START = "subtaskStart";
  private static final String PHASE_SUB_END = "subtaskEnd";
  private static final String HEADERS_NAME = "headers";
  private static final String PARAMETERS_NAME = "parameters";
  private static final String COOKIES_NAME = "cookies";
  private static final String HTTP_STATUS_NAME = "httpStatus";
  private static final String CONTENT_TYPE_NAME = "contentType";
  private static final String CONTENT_LENGTH_NAME = "contentLength";
  private static final String DURATION_MS_NAME = "durationMs";
  private static final String DURATION_NANO_NAME = "durationNano";
  private static final String HTTP_RESPONSE_NAME = "httpResponse";
  private static final String NESTED_EXCEPTIONS = "nestedExceptions";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  static final Set<String> subStringsToObfuscate = new HashSet(Arrays
      .asList("authid", "token_type", "auth_type", "secret", "pass", "token", "ticket", "tkt", "cred", "auth", "key",
          "clientcontext", "login"));

  public EventLogger() {
  }

  public static Marker addPayloadData(Logger logger, TransactionContext transContext, HttpServletRequest httpReq,
      String payload) {
    ObjectNode jsonPayload;
    if (httpReq != null) {
      jsonPayload = ContextFactory.getJsonPayload(httpReq, payload);
    } else {
      jsonPayload = ContextFactory.getJson(payload);
    }
    return MarkerUtil.addPayloadData(jsonPayload, payload);
  }

  public static void logPayload(Logger logger, TransactionContext transContext, String payload, String message,
      LogNameValuePair... nvPairs) {
    Marker logData = generateMarker("intermediate", transContext, null, nvPairs);
    Marker payloadData = addPayloadData(logger, transContext, null, payload);
    if (payloadData != null) {
      logData.add(payloadData);
      logger.info(logData, message);
    }

  }


  static Map<String, Object> toHeaderMap(Response response) {
    MultivaluedMap<String, String> headers = response.getStringHeaders();
    if (headers == null) {
      return new HashMap();
    } else {
      Map<String, Object> resHeaders = new HashMap(headers.size());
      headers.forEach((k, v) -> {
        resHeaders.put(k, v.size() == 1 ? v.get(0) : v);
      });
      return resHeaders;
    }
  }

  //--------

  public static Marker generateMarker(String phase, TransactionContext transContext, Marker subContext,
      LogNameValuePair... nvPairs) {
    return MarkerUtil.generateMarker(phase, transContext, subContext, nvPairs);
  }

  public static void info(Logger logger, TransactionContext transContext, String message,
      LogNameValuePair... nvPairs) {
    logger.info(generateMarker(PHASE_INTERMEDIATE, transContext, null, nvPairs), message);
  }

  public static void warn(Logger logger, TransactionContext transContext, String message,
      LogNameValuePair... nvPairs) {
    logger.warn(generateMarker(PHASE_INTERMEDIATE, transContext, null, nvPairs), message);
  }

  public static void warn(Logger logger, TransactionContext transContext, String message, Throwable throwable,
      LogNameValuePair... nvPairs) {
    Marker extCtxMarker = Markers.append("nestedExceptions", getNestedExceptions(throwable));
    logger.warn(generateMarker(PHASE_INTERMEDIATE, transContext, extCtxMarker, nvPairs), message, throwable);
  }

  public static void error(Logger logger, TransactionContext transContext, String message, Throwable throwable,
      LogNameValuePair... nvPairs) {
    Marker extCtxMarker = Markers.append("nestedExceptions", getNestedExceptions(throwable));
    logger.error(generateMarker(PHASE_INTERMEDIATE, transContext, extCtxMarker, nvPairs), message, throwable);
  }

  public static void error(Logger logger, TransactionContext transContext, String message,
      LogNameValuePair... nvPairs) {
    logger.error(generateMarker(PHASE_INTERMEDIATE, transContext, null, nvPairs), message);
  }

  public static void subtaskStart(Logger logger, TransactionContext transContext, LoggingContext subContext,
      String message, LogNameValuePair... nvPairs) {
    Marker subCtxMarker = Markers.append(SUB_CTX_NAME, subContext);
    logger.info(generateMarker(PHASE_SUB_START, transContext, subCtxMarker, nvPairs), message);
  }

  public static void subtaskStart(Logger logger, TransactionContext transContext, LoggingContext subContext,
      LogNameValuePair... nvPairs) {
    subtaskStart(logger, transContext, subContext, null, nvPairs);
  }

  public static void subtaskEnd(Logger logger, TransactionContext transContext, LoggingContext subContext,
      String message, LogNameValuePair... nvPairs) {
    Map<String, Object> extCtxMap = objectMapper
        .convertValue(subContext, new TypeReference<Map<String, Object>>() {
        });
    extCtxMap.put(DURATION_MS_NAME, System.currentTimeMillis() - subContext.getMsStart());
    extCtxMap.put(DURATION_NANO_NAME, System.nanoTime() - subContext.getNanoStart());
    Marker extCtxMarker = Markers.append(SUB_CTX_NAME, extCtxMap);
    logger.info(generateMarker(PHASE_SUB_END, transContext, extCtxMarker, nvPairs), message);
  }

  public static void subtaskEnd(Logger logger, TransactionContext transContext, LoggingContext subContext,
      LogNameValuePair... nvPairs) {
    subtaskEnd(logger, transContext, subContext, null, nvPairs);
  }

  public static void start(Logger logger, HttpServletRequest httpReq, String payload, TransactionContext transContext,
      String message, LogNameValuePair... nvPairs) {

    Marker logData = generateMarker("start", transContext, null, nvPairs);

    if (httpReq != null) {
      logData.add(Markers.append(HEADERS_NAME, getRequestHeaders(httpReq)));
      logData.add(Markers.append(PARAMETERS_NAME, getQueryParameters(httpReq)));
      logData.add(Markers.append(COOKIES_NAME, getRequestCookies(httpReq)));

      if (httpReq.getMethod().equals("POST")) {
        Marker payloadMarker = addPayloadData(logger, transContext, httpReq, payload);
        if (payloadMarker != null) {
          logData.add(payloadMarker);
        }
      }
    }

    logger.info(logData, message);
  }

  public static void start(Logger logger, HttpServletRequest httpReq, String payload, TransactionContext transContext,
      LogNameValuePair... nvPairs) {
    start(logger, httpReq, payload, transContext, null, nvPairs);
  }

  public static void start(Logger logger, HttpServletRequest httpReq, TransactionContext transContext, String message,
      LogNameValuePair... nvPairs) {
    start(logger, httpReq, null, transContext, message, nvPairs);
  }

  public static void start(Logger logger, HttpServletRequest httpReq, TransactionContext transContext,
      LogNameValuePair... nvPairs) {
    start(logger, httpReq, null, transContext, null, nvPairs);
  }

  public static void startWithoutHttpServletRequest(Logger logger, TransactionContext transContext, String message, LogNameValuePair... nvPairs) {
    start(logger, null, null, transContext, message, nvPairs);
  }

  public static void end(Logger logger, Response resp, TransactionContext transContext, String message,
      LogNameValuePair... nvPairs) {
    Marker logData = generateMarker(PHASE_END, transContext, null, nvPairs);
    if (resp != null) {
      int httpStatus = resp.getStatus();
      logData.add(Markers.append(HTTP_STATUS_NAME, httpStatus));
      if (resp.getMediaType() != null) {
        logData.add(Markers.append(CONTENT_TYPE_NAME, resp.getMediaType().getType()));
      }

      logData.add(Markers.append(CONTENT_LENGTH_NAME, resp.getEntity().toString().length()));
      logData.add(Markers.append(HEADERS_NAME, toHeaderMap(resp)));
    }

    logData.add(Markers.append(DURATION_MS_NAME, System.currentTimeMillis() - transContext.getMsStart()));
    logData.add(Markers.append(DURATION_NANO_NAME, System.nanoTime() - transContext.getNanoStart()));
    logData.add(Markers.append(HTTP_RESPONSE_NAME, resp));
    logger.info(logData, message);
  }

  public static void end(Logger logger, HttpServletResponse httpResp, TransactionContext transContext, String message,
      LogNameValuePair... nvPairs) {

    Marker logData = generateMarker(PHASE_END, transContext, null, nvPairs);
    int httpStatus = httpResp.getStatus();
    logData.add(Markers.append(HTTP_STATUS_NAME, httpStatus));
    String contentType = httpResp.getContentType();
    if (StringUtils.isNotEmpty(contentType)) {
      logData.add(Markers.append(CONTENT_TYPE_NAME, contentType));
    }

    String contentLength = httpResp.getHeader(CONTENT_LENGTH_NAME);
    if (StringUtils.isNotEmpty(contentLength)) {
      logData.add(Markers.append(CONTENT_LENGTH_NAME, contentLength));
    }

    logData.add(Markers.append(HEADERS_NAME, getResponseHeaders(httpResp)));
    logData.add(Markers.append(DURATION_MS_NAME, System.currentTimeMillis() - transContext.getMsStart()));
    logData.add(Markers.append(DURATION_NANO_NAME, System.nanoTime() - transContext.getNanoStart()));
    logger.info(logData, message);
  }

  public static void end(Logger logger, Response resp, TransactionContext transContext, LogNameValuePair... nvPairs) {
    end(logger, resp, transContext, null, nvPairs);
  }

  public static void end(Logger logger, TransactionContext transContext, String message, LogNameValuePair... nvPairs) {
    Marker logData = generateMarker(PHASE_END, transContext, null, nvPairs);
    logData.add(Markers.append(DURATION_MS_NAME, System.currentTimeMillis() - transContext.getMsStart()));
    logData.add(Markers.append(DURATION_NANO_NAME, System.nanoTime() - transContext.getNanoStart()));
    logger.info(logData, message);
  }

  static Map<String, Object> getResponseHeaders(HttpServletResponse response) {
    if (response != null && response.getHeaderNames() != null) {
      Collection<String> headerNames = response.getHeaderNames();
      Map<String, Object> headers = new HashMap(headerNames.size());
      Iterator var3 = headerNames.iterator();

      while (var3.hasNext()) {
        String header = (String) var3.next();
        Collection<String> headerValues = response.getHeaders(header);
        headers.put(header, headerValues.size() == 1 ? headerValues.iterator().next() : headerValues);
      }

      return headers;
    } else {
      return new HashMap();
    }
  }

  /**
   * Extracts Headers from the Http Request and returns them in a Map
   *
   * @param httpReq Http Request
   * @return Headers to be logged
   */
  static Map<String, String> getRequestHeaders(HttpServletRequest httpReq) {
    Map<String, String> headers = new HashMap<>();
    try {
      Enumeration<String> headerNames = httpReq.getHeaderNames();
      if (headerNames == null) {
        return headers;
      }

      while (headerNames.hasMoreElements()) {
        String name = headerNames.nextElement();
        Enumeration<String> values = httpReq.getHeaders(name);
        while (values.hasMoreElements()) {
          String value = values.nextElement();
          if (name.equalsIgnoreCase(COOKIE)) {
            Cookie[] cookies = httpReq.getCookies();
            if (cookies != null) {
              StringBuilder cookieString = new StringBuilder();
              for (Cookie cookie : cookies) {
                cookieString
                    .append(cookie.getName())
                    .append("=")
                    .append(cookie.getValue())
                    .append(" ");
              }
              headers.put(name, cookieString.toString().trim());
            }
            // Parse and obfuscate the rest of the Header
          } else {
            headers.put(name, value);
          }
        }
      }
    } catch (Exception e) {
      warn(LOGGER, null, "Error Parsing Headers", e);
    }

    return headers;
  }


  private static Map<String, String> getQueryParameters(HttpServletRequest httpReq) {
    HashMap parameters = new HashMap();

    try {
      Enumeration<String> paramNames = httpReq.getParameterNames();
      if (paramNames == null) {
        return parameters;
      }

      while (paramNames.hasMoreElements()) {
        String name = (String) paramNames.nextElement();
        String value = httpReq.getParameter(name);
        parameters.put(name, value);
      }
    } catch (Exception var5) {
      warn(LOGGER, (TransactionContext) null, (String) "Error Parsing Query Parameters", (Throwable) var5);
    }

    return parameters;
  }

  static Map<String, String> getRequestCookies(HttpServletRequest httpReq) {
    Map<String, String> cookiesMap = new HashMap();
    (Optional.ofNullable(httpReq).
        map(HttpServletRequest::getCookies)
        .map(Arrays::stream)
        .orElseGet(Stream::empty))
        .forEach((cookie) -> {
          cookiesMap.put(cookie.getName(), cookie.getValue());
        });
    return cookiesMap;
  }

  private static String getNestedExceptions(Throwable throwable) {
    StringBuilder sb = new StringBuilder();
    Throwable root = throwable;

    while (root != null) {
      sb.append(root.getClass().getCanonicalName()).append(": ").append(root.getMessage());
      if ((root = root.getCause()) != null) {
        sb.append("\n\t");
      }
    }

    return sb.toString();
  }
}
