package com.intuit.graphql.gateway.graphql;

import com.intuit.graphql.gateway.logging.EventLogger;
import com.intuit.graphql.gateway.logging.interfaces.TransactionContext;
import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import java.util.concurrent.CompletionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.context.Context;

/**
 * This class allows us to intercept all exceptions produced while executing a graphql query.
 */
@Slf4j
@Component
public class CustomDataFetcherExceptionHandler implements DataFetcherExceptionHandler {

  @Override
  public DataFetcherExceptionHandlerResult onException(final DataFetcherExceptionHandlerParameters handlerParameters) {
    Throwable exception = handlerParameters.getException();

    //attempt to unwrap completion exceptions for error objects
    Throwable cause =
        exception instanceof CompletionException && exception.getCause() != null ? exception.getCause() : exception;
    SourceLocation sourceLocation = handlerParameters.getSourceLocation();
    ExecutionPath path = handlerParameters.getPath();

    ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(path, cause, sourceLocation);

    try {
      final TransactionContext tx = handlerParameters
          .getDataFetchingEnvironment().<GraphQLContext>getContext().<Context>get(Context.class)
          .get(TransactionContext.class);

      EventLogger.warn(log, tx, exception.getMessage(), exception);
    } catch (Exception unused) {
      log.warn(error.getMessage(), exception);
    }

    return DataFetcherExceptionHandlerResult.newResult(error).build();
  }
}
