package com.intuit.graphql.gateway.webclient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Specify the type of GraphQL request being issued. This allows for fine-grained metrics capturing based on the GraphQL
 * request.
 *
 * n.b. We may end up using the built-in GraphQL {@link graphql.language.OperationDefinition.Operation} type to
 * represent request types.
 */
public enum RequestType {
  INTROSPECTION,
  QUERY,
  MUTATION; //necessary?

  private static final Set<RequestType> REQUEST_TYPES = new HashSet<>();

  static {
    REQUEST_TYPES.addAll(Arrays.asList(RequestType.values()));
  }
}
