package com.intuit.graphql.gateway.authorization;

import static com.intuit.graphql.gateway.webclient.TxProvider.getTx;

import com.intuit.graphql.authorization.enforcement.SimpleAuthZListener;
import com.intuit.graphql.gateway.logging.EventLogger;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.schema.GraphQLTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AuthZLogListener extends SimpleAuthZListener {

  @Override
  public void onFieldRedaction(ExecutionContext executionContext, QueryVisitorFieldEnvironment env) {
    getTx(executionContext.getContext())
        .ifPresent(tx -> EventLogger.error(log, tx, String.format("RedactedTypeField='%s/%s of type: %s'",
            GraphQLTypeUtil.unwrapAll(env.getParentType()).getName(),
            env.getField().getName(),
            GraphQLTypeUtil.simplePrint(env.getFieldDefinition().getType()))));
  }
}
