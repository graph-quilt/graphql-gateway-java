package com.intuit.graphql.gateway.utils;

import static com.intuit.graphql.gateway.utils.SchemaDiffUtil.hasNoDiff;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.registry.SchemaDifferenceMetrics;
import graphql.schema.diff.DiffEvent;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SchemaDiffUtilTest {

  @Mock
  private DiffEvent mockDiffEvent;

  @Mock
  private SchemaDifferenceMetrics mockSchemaDifferenceMetrics;

  public void setup() {
    when(mockSchemaDifferenceMetrics.getInfos()).thenReturn(Collections.emptyList());
    when(mockSchemaDifferenceMetrics.getBreakages()).thenReturn(Collections.emptyList());
    when(mockSchemaDifferenceMetrics.getDangers()).thenReturn(Collections.emptyList());
  }

  @Test
  public void hasNoDiff_infosIsNotEmpty_returnsFalse() {
    when(mockSchemaDifferenceMetrics.getInfos()).thenReturn(Arrays.asList(mockDiffEvent));

    boolean actualBoolean = hasNoDiff(mockSchemaDifferenceMetrics);

    assertThat(actualBoolean).isFalse();
  }

  @Test
  public void hasNoDiff_breakagesIsNotEmpty_returnsFalse() {
    when(mockSchemaDifferenceMetrics.getBreakages()).thenReturn(Arrays.asList(mockDiffEvent));

    boolean actualBoolean = hasNoDiff(mockSchemaDifferenceMetrics);

    assertThat(actualBoolean).isFalse();
  }

  @Test
  public void hasNoDiff_dangersIsNotEmpty_returnsFalse() {
    when(mockSchemaDifferenceMetrics.getDangers()).thenReturn(Arrays.asList(mockDiffEvent));

    boolean actualBoolean = hasNoDiff(mockSchemaDifferenceMetrics);

    assertThat(actualBoolean).isFalse();
  }

  @Test
  public void hasNoDiff_allIsEmpty_returnsTrue() {
    boolean actualBoolean = hasNoDiff(mockSchemaDifferenceMetrics);

    assertThat(actualBoolean).isTrue();
  }

}
