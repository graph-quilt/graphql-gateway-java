package com.intuit.graphql.gateway.introspection;

import static graphql.introspection.Introspection.SchemaMetaFieldDef;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.GraphQLContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.util.context.Context;

@AllArgsConstructor
@Slf4j
public class IntrospectionInstrumentation extends SimpleInstrumentation {

  @Override
  public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> originalDataFetcher,
      InstrumentationFieldFetchParameters parameters) {

    DataFetchingEnvironment dataFetchingEnvironment = parameters.getEnvironment();

    Context context = ((GraphQLContext) dataFetchingEnvironment.getContext()).get(Context.class);
    TransactionContext tx = context.get(TransactionContext.class);

    if (isSchemaIntrospection(dataFetchingEnvironment)) {
      EventLogger.error(log, tx, "Schema Introspection Detected.  Request aborted.");
      return new AbortIntrospectionDataFetcher();
    }

    return originalDataFetcher;
  }

  private boolean isSchemaIntrospection(DataFetchingEnvironment dataFetchingEnvironment) {
    GraphQLObjectType queryType = dataFetchingEnvironment.getGraphQLSchema().getQueryType();
    GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) dataFetchingEnvironment.getParentType();
    Field field = dataFetchingEnvironment.getField();

    return StringUtils.equals(parentType.getName(), queryType.getName())
        && StringUtils.equals(field.getName(), SchemaMetaFieldDef.getName());
  }

}
