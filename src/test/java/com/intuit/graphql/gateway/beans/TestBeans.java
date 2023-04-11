package com.intuit.graphql.gateway.beans;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
public class TestBeans {

  @Bean
  @Primary
  public S3AsyncClient mockAsyncClient() {
    return Mockito.mock(S3AsyncClient.class);
  }
}
