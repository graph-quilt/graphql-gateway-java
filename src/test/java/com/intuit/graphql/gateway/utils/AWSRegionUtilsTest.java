package com.intuit.graphql.gateway.utils;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.intuit.graphql.gateway.s3.AWSRegion;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

public class AWSRegionUtilsTest {

  @Test
  public void getRegionRegionHeaderNotPresentReturnsDefault() {
    // GIVEN
    HttpHeaders httpHeaders = HttpHeaders.EMPTY;

    // WHEN
    AWSRegion awsRegion = AWSRegionUtils.getRegionFrom(httpHeaders);

    // THEN
    assertThat(awsRegion).isEqualTo(AWSRegion.DEFAULT);
  }

  @Test
  public void getRegionRegionHeaderHasDefaultReturnsDefault() {
    // GIVEN
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(AWSRegionUtils.HEADER_INTUIT_AWS_REGION, "default");

    // WHEN
    AWSRegion awsRegion = AWSRegionUtils.getRegionFrom(httpHeaders);

    // THEN
    assertThat(awsRegion).isEqualTo(AWSRegion.DEFAULT);
  }

  @Test
  public void getRegionRegionHeaderHasUsWest2ReturnsUsWest2() {
    // GIVEN
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(AWSRegionUtils.HEADER_INTUIT_AWS_REGION, "us-west-2");

    // WHEN
    AWSRegion awsRegion = AWSRegionUtils.getRegionFrom(httpHeaders);

    // THEN
    assertThat(awsRegion).isEqualTo(AWSRegion.US_WEST_2);
  }

  @Test
  public void getRegionRegionHeaderHasUsEast2ReturnsUsEast2() {
    // GIVEN
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(AWSRegionUtils.HEADER_INTUIT_AWS_REGION, "us-east-2");

    // WHEN
    AWSRegion awsRegion = AWSRegionUtils.getRegionFrom(httpHeaders);

    // THEN
    assertThat(awsRegion).isEqualTo(AWSRegion.US_EAST_2);
  }

}
