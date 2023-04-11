package com.intuit.graphql.gateway.s3;

import java.util.Objects;
import lombok.Getter;

@Getter
public enum AWSRegion {
  US_WEST_2("us-west-2"),
  US_EAST_2("us-east-2"),
  DEFAULT("default");
  private String stringValue;

  AWSRegion(String stringValue) {
    this.stringValue = stringValue;
  }

  public static AWSRegion getValueFor(String name){
    if (Objects.isNull(name)) {
      return DEFAULT;
    }
    switch (name) {
      case "us-west-2":
        return US_WEST_2;
      case "us-east-2":
        return US_EAST_2;
      default:
        return DEFAULT;
    }
  }
}
