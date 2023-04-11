package com.intuit.graphql.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MapperTests {

  @Test
  public void mapperReturnsSameMapperTest() {
    Assertions.assertThat(Mapper.mapper()).isEqualTo(Mapper.mapper());
  }

  @Test
  public void yamlMapperReturnsSameMapperTest() {
    Assertions.assertThat(Mapper.mapper()).isSameAs(Mapper.mapper());
  }
}
