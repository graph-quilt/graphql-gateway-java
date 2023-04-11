package com.intuit.graphql.gateway.registry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import graphql.schema.diff.DiffEvent;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.ToString;

@Builder
@Data
public class SchemaDifferenceMetrics {
  @JsonIgnore
  @ToString.Exclude
  private ServiceRegistration serviceRegistration;

  @Singular
  private  List<DiffEvent> infos;
  @Singular
  private  List<DiffEvent> breakages;
  @Singular
  private  List<DiffEvent> dangers;
}
