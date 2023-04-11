package com.intuit.graphql.gateway.s3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * This class is part of {@link RuleConfig} and is used to deserialized a field coordinate from config.yml.
 */
@EqualsAndHashCode
@Getter
public class FieldCoordinateConfig {

  private final String parentTypename;

  private final String fieldName;

  @JsonCreator
  public FieldCoordinateConfig(@JsonProperty(value="parentTypename", required = true) String parentTypename,
      @JsonProperty(value="fieldName", required = true) String fieldName) {
    this.parentTypename = parentTypename;
    this.fieldName = fieldName;
  }

  @Override
  public String toString() {
    return String.format("%s:%s", parentTypename, fieldName);
  }

}
