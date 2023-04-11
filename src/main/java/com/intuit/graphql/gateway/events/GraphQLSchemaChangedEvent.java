package com.intuit.graphql.gateway.events;

import org.springframework.context.ApplicationEvent;

public class GraphQLSchemaChangedEvent extends ApplicationEvent {

  public static final GraphQLSchemaChangedEvent INSTANCE = new GraphQLSchemaChangedEvent(new Object());

  public GraphQLSchemaChangedEvent(final Object source) {
    super(source);
  }
}
