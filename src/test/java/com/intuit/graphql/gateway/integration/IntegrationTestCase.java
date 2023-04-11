package com.intuit.graphql.gateway.integration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class IntegrationTestCase {

  public static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  public static int WIREMOCK_PORT = 4040;

  protected TestRestTemplate restTemplate;
  protected HttpHeaders headers;
  protected int port;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

  static {
    OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
  }

  @Before
  public void initialize() {
    port = 7000;
    headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    restTemplate = new TestRestTemplate();
  }

  @After
  public void end() {
  }
}
