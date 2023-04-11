package com.intuit.graphql.gateway.s3;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Getter
@AllArgsConstructor
public class S3ClientProvider {

  private final Map<String, S3AsyncClient> bucketClientMap;

}
