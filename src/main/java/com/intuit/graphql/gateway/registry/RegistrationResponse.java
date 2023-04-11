package com.intuit.graphql.gateway.registry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonInclude(Include.NON_NULL)
public class RegistrationResponse {
  SchemaDifferenceMetrics schemaDiff;
  Map<String,String> files;
  String message;
}
