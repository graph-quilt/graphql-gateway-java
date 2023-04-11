package com.intuit.graphql.gateway.utils;

import com.intuit.graphql.gateway.s3.AWSRegion;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

public class AWSRegionUtils {

  public static final String HEADER_INTUIT_AWS_REGION = "intuit_aws_region";

  public static AWSRegion getRegionFrom(HttpHeaders headers) {
    String awsRegionString = headers.getFirst(HEADER_INTUIT_AWS_REGION);
    if (StringUtils.isBlank(awsRegionString)) {
      return AWSRegion.DEFAULT;
    }
    return AWSRegion.getValueFor(awsRegionString);
  }

}
