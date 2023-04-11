package com.intuit.graphql.gateway.s3;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.Test;

public class FieldCoordinateConfigTest {

  private final FieldCoordinateConfig specificationUnderTest =
      new FieldCoordinateConfig("ParentTypename", "fieldName");

  @Test
  public void canCreateInstance() {
    assertThat(specificationUnderTest.getParentTypename()).isEqualTo("ParentTypename");
    assertThat(specificationUnderTest.getFieldName()).isEqualTo("fieldName");
  }

  @Test
  public void toStringTest() {
    String actual = specificationUnderTest.toString();
    assertThat(actual).isEqualTo("ParentTypename:fieldName");
  }

}
