package com.intuit.graphql.gateway.handler;

import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class GraphiQLRouteHandlerTests {

  @Mock
  private ResourceLoader resourceLoader;
  @Mock
  Resource resource;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = IOException.class)
  public void throwsExceptionOnErrorLoadingGraphiQLTest() throws IOException {
    when(resourceLoader.getResource(Mockito.anyString())).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(new IOException());
    new GraphiQLRouteHandler(resourceLoader);
  }
}
