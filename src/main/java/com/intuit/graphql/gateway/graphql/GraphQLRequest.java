package com.intuit.graphql.gateway.graphql;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.springframework.core.ParameterizedTypeReference;

@Value.Immutable
@JsonSerialize(as = ImmutableGraphQLRequest.class)
@JsonDeserialize(as = ImmutableGraphQLRequest.class)
public interface GraphQLRequest {

  ParameterizedTypeReference<Map<String, Object>> SPECIFICATION_TYPE_REFERENCE = new ParameterizedTypeReference<Map<String, Object>>() {
  };

  String query();

  @Nullable
  HashMap<String, Object> variables();

  @Nullable
  String operationName();
}
