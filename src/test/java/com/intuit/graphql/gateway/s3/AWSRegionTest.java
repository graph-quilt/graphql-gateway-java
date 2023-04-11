package com.intuit.graphql.gateway.s3;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

public class AWSRegionTest {

  private AWSRegion awsRegion;

  @Test
  public void AWSRegion_US_WEST_2_givesCorrectStringValue() {
    awsRegion = AWSRegion.US_WEST_2;
    assertThat(awsRegion.getStringValue()).isEqualTo("us-west-2");
  }

  @Test
  public void AWSRegion_US_EAST_2_givesCorrectStringValue() {
    awsRegion = AWSRegion.US_EAST_2;
    assertThat(awsRegion.getStringValue()).isEqualTo("us-east-2");
  }

  @Test
  public void AWSRegion_DEFAULT_givesCorrectStringValue() {
    awsRegion = AWSRegion.DEFAULT;
    assertThat(awsRegion.getStringValue()).isEqualTo("default");
  }

  @Test
  public void getValueForDefault_returnsDEFAULT() {
    assertThat(AWSRegion.getValueFor("default")).isEqualTo(AWSRegion.DEFAULT);
  }

  @Test
  public void getValueForUsEast2_returnsUS_EAST_2() {
    assertThat(AWSRegion.getValueFor("us-east-2")).isEqualTo(AWSRegion.US_EAST_2);
  }

  @Test
  public void getValueForUsWest2_returnsUS_WEST_2() {
    assertThat(AWSRegion.getValueFor("us-west-2")).isEqualTo(AWSRegion.US_WEST_2);
  }

  @Test
  public void getValueForNull_returnsUS_WEST_2() {
    assertThat(AWSRegion.getValueFor(null)).isEqualTo(AWSRegion.DEFAULT);
  }

  @Test
  public void getValueForSomeRandonValue_returnsUS_WEST_2() {
    assertThat(AWSRegion.getValueFor("SomeRandomValue")).isEqualTo(AWSRegion.DEFAULT);
  }

}

