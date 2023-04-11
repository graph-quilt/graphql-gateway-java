package com.intuit.graphql.gateway.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.graphql.gateway.logging.interfaces.ContextFactoryInput;
import com.intuit.graphql.gateway.logging.interfaces.ExternalContext;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableExternalContext;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableSubtaskContext;
import com.intuit.graphql.gateway.logging.interfaces.ImmutableTransactionContext;
import com.intuit.graphql.gateway.logging.interfaces.SubtaskContext;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import java.util.UUID;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

public class ContextFactory {

  public static final String HEADER_APPID = "appid";
  private static final ObjectMapper objectMapper = new ObjectMapper();


  private ContextFactory() {
  }

  public static TransactionContext getTransactionContext(@NonNull ContextFactoryInput input) {
    if (input == null) {
      throw new NullPointerException("input");
    } else {
      ObjectNode jsonPayload = getJsonPayload(input.method(), input.contentType(), input.payload());
      String tid = extractOrGenerateTid(jsonPayload, input.headerValue());
      return getTransactionContext(input.headerValue(), jsonPayload, input.app(), input.version(),
          input.env(), input.method(), input.requestUri(), input.contentType(), tid);
    }
  }

  public static TransactionContext getTransactionContext(HttpServletRequest httpReq, String payload, String app,
      String version, String env) {
    String method = httpReq.getMethod();
    String path = httpReq.getRequestURI();
    String contentType = httpReq.getContentType();
    ObjectNode jsonPayload = getJsonPayload(method, httpReq.getContentType(), payload);
    httpReq.getClass();
    String tid = extractOrGenerateTid(jsonPayload, httpReq::getHeader);
    httpReq.setAttribute("tid", tid);
    return getTransactionContext(httpReq::getHeader, jsonPayload, app, version, env, method, path,
        contentType, tid);
  }

  public static TransactionContext getTransactionContext(String method, String path, String app, String version,
      String env) {
    String tid = UUID.randomUUID().toString();
    return ImmutableTransactionContext.builder().app(app).env(env).version(version).tid(tid)
        .method(method).path(path).build();
  }

  private static TransactionContext getTransactionContext(Function<String, String> headerProvider,
      ObjectNode jsonPayload, String app, String version, String env, String method,
      String uri, String contentType, String tid) {
    String ipAddress = getIpAddress(headerProvider);
    String appId = getPayloadOrHeaderValue(jsonPayload, headerProvider, HEADER_APPID);
    return ImmutableTransactionContext.builder().app(app).env(env).version(version)
        .tid(tid).method(method).path(uri).contentType(contentType).appId(appId).ipAddress(ipAddress).build();
  }

  public static SubtaskContext getSubtaskContext(String action) {
    return ImmutableSubtaskContext.of(action);
  }

  public static ExternalContext getExternalContext(String initiatedBy, String method, String url) {
    return ImmutableExternalContext.builder().initiatedBy(initiatedBy).method(method).url(url).build();
  }

  public static ObjectNode getJson(String str) {
    if (StringUtils.isEmpty(str)) {
      return null;
    } else {
      try {
        return (ObjectNode) objectMapper.readTree(str);
      } catch (Exception var2) {
        return null;
      }
    }
  }

  public static ObjectNode getJsonPayload(HttpServletRequest httpReq, String payload) {
    return getJsonPayload(httpReq.getMethod(), httpReq.getContentType(), payload);
  }

  private static ObjectNode getJsonPayload(String method, String contentType, String payload) {
    return "POST".equals(method) && "application/json".equals(contentType) ? getJson(payload) : null;
  }

  private static String getPayloadValue(ObjectNode jsonPayload, String name) {
    String value = null;
    if (jsonPayload != null && jsonPayload.get(name) != null) {
      value = jsonPayload.get(name).asText();
    }

    return StringUtils.isNotEmpty(value) ? value : null;
  }

  private static String getPayloadOrHeaderValue(ObjectNode jsonPayload, Function<String, String> headerProvider,
      String name) {
    String value = getPayloadValue(jsonPayload, name);
    if (StringUtils.isEmpty(value)) {
      value = getHeaderValue(headerProvider, name);
    }

    return value;
  }

  private static String getHeaderValue(Function<String, String> headerProvider, String name) {
    String value = (String) headerProvider.apply(name);
    if (StringUtils.isNotEmpty(value)) {
      value = value.replaceAll("(\\r|\\n)", "");
    }

    return value;
  }

  private static String getIpAddress(Function<String, String> headerProvider) {
    return headerProvider.apply("x-forwarded-for");
  }

  private static String extractOrGenerateTid(ObjectNode jsonPayload, Function<String, String> headerProvider) {
    String tid = getPayloadOrHeaderValue(jsonPayload, headerProvider, "tid");
    if (StringUtils.isEmpty(tid)) {
      tid = UUID.randomUUID().toString();
    }

    return tid;
  }
}
