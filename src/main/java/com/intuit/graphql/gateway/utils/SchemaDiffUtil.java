package com.intuit.graphql.gateway.utils;

import com.intuit.graphql.gateway.registry.SchemaDifferenceMetrics;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;

public class SchemaDiffUtil {

  private SchemaDiffUtil() {}

  public static boolean hasNoDiff(SchemaDifferenceMetrics schemaDifferenceMetrics) {
    Objects.requireNonNull(schemaDifferenceMetrics, "schemaDifferenceMetrics is required.");
    // based on tests, infos, breakages and dangers are empty if no diff.
    return CollectionUtils.isEmpty(schemaDifferenceMetrics.getInfos())
        && CollectionUtils.isEmpty(schemaDifferenceMetrics.getBreakages())
        && CollectionUtils.isEmpty(schemaDifferenceMetrics.getDangers());
  }

}
