package com.intuit.graphql.gateway.s3;

import com.intuit.graphql.gateway.s3.S3Configuration.S3Bucket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

@Configuration
@EnableConfigurationProperties(S3Configuration.class)
@Slf4j
public class S3ClientConfiguration {


  private final S3Configuration s3Configuration;

  public S3ClientConfiguration(final S3Configuration s3Configuration) {
    this.s3Configuration = s3Configuration;
  }

  @Bean
  public S3ClientProvider getS3ClientMap() throws URISyntaxException {
    Map<String, S3AsyncClient> bucketClientMap = new HashMap<>();
    for (String key : s3Configuration.getBuckets().keySet()) {
      S3Bucket s3Bucket = s3Configuration.getBuckets().get(key);
      S3AsyncClient s3AsyncClient = createClient(s3Bucket.getRegion(), s3Bucket.getEndpoint());
      bucketClientMap.put(s3Bucket.getBucketName(), s3AsyncClient);
    }
    return new S3ClientProvider(bucketClientMap);
  }


  public S3AsyncClient createClient(String region, String endpoint) throws URISyntaxException {
    S3AsyncClientBuilder builder = S3AsyncClient.builder().region(Region.of(region))
        .endpointOverride(new URI(endpoint));
    return builder.build();
  }

  @Bean
  @ConditionalOnMissingBean
  public S3AsyncClient s3AsyncClient(S3ClientProvider s3ClientProvider) {
    return s3ClientProvider.getBucketClientMap().get(s3Configuration.getPolling().getBucketname());
  }

}
