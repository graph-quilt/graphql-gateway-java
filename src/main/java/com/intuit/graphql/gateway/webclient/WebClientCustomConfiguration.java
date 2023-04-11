package com.intuit.graphql.gateway.webclient;

import com.intuit.graphql.gateway.config.properties.WebClientProperties;
import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.webfilter.RequestLoggingExchangeFilter;
import io.netty.channel.ChannelOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientCustomConfiguration {

  @Autowired
  WebClientProperties webClientProperties;

  @Bean
  public WebClient.Builder overrideWebClientBuilder() {
    HttpClient httpClient = HttpClient.create()
        .tcpConfiguration(client ->
            client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientProperties.getConnectTimeout()));

    int codecMaxInMemorySize = webClientProperties.getCodecMaxInMemorySizeInMbytes() * 1024 * 1024;
    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(codecMaxInMemorySize))
        .build();

    return WebClient.builder()
        .exchangeStrategies(exchangeStrategies)
        .clientConnector(new ReactorClientHttpConnector(httpClient));
  }

  @Bean
  @ConditionalOnClass(EventLogger.class)
  @ConditionalOnMissingBean
  public RequestLoggingExchangeFilter requestLoggingExchangeFilter() {
    return new RequestLoggingExchangeFilter();
  }

  @Bean
  @ConditionalOnMissingBean
  public WebClient defaultWebClient(List<ExchangeFilterFunction> filterFunctions, WebClient.Builder webClientBuilder) {
    return webClientBuilder
        .filters(list -> list.addAll(filterFunctions))
        .build();
  }
}
