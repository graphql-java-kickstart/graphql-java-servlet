package graphql.kickstart.servlet;

import static graphql.kickstart.servlet.HttpRequestHandler.STATUS_BAD_REQUEST;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class HttpRequestInvokerImpl implements HttpRequestInvoker {

  private final GraphQLConfiguration configuration;
  private final GraphQLInvoker graphQLInvoker;
  private final QueryResponseWriterFactory queryResponseWriterFactory;

  @Override
  public void execute(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (request.isAsyncSupported()) {
      AsyncContext asyncContext =
          request.isAsyncStarted()
              ? request.getAsyncContext()
              : request.startAsync(request, response);
      asyncContext.setTimeout(configuration.getAsyncTimeout());
      invoke(invocationInput, request, response)
          .thenAccept(result -> writeResultResponse(invocationInput, result, request, response))
          .exceptionally(t -> writeErrorResponse(t, response))
          .thenAccept(aVoid -> asyncContext.complete());
    } else {
      try {
        GraphQLQueryResult result = invoke(invocationInput, request, response).join();
        writeResultResponse(invocationInput, result, request, response);
      } catch (Exception t) {
        writeErrorResponse(t, response);
      }
    }
  }

  private void writeResultResponse(
      GraphQLInvocationInput invocationInput,
      GraphQLQueryResult queryResult,
      HttpServletRequest request,
      HttpServletResponse response) {
    QueryResponseWriter queryResponseWriter = createWriter(invocationInput, queryResult);
    try {
      queryResponseWriter.write(request, response);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected QueryResponseWriter createWriter(
      GraphQLInvocationInput invocationInput, GraphQLQueryResult queryResult) {
    return queryResponseWriterFactory.createWriter(invocationInput, queryResult, configuration);
  }

  private Void writeErrorResponse(Throwable t, HttpServletResponse response) {
    response.setStatus(STATUS_BAD_REQUEST);
    log.info(
        "Bad request: path was not \"/schema.json\" or no query variable named \"query\" given", t);
    return null;
  }

  private CompletableFuture<GraphQLQueryResult> invoke(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return graphQLInvoker.queryAsync(invocationInput);
    }
    return invokeBatched((GraphQLBatchedInvocationInput) invocationInput, request, response);
  }

  private CompletableFuture<GraphQLQueryResult> invokeBatched(
      GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    BatchInputPreProcessor preprocessor = configuration.getBatchInputPreProcessor();
    BatchInputPreProcessResult result =
        preprocessor.preProcessBatch(batchedInvocationInput, request, response);
    if (result.isExecutable()) {
      return graphQLInvoker.queryAsync(result.getBatchedInvocationInput());
    }

    return CompletableFuture.completedFuture(
        GraphQLQueryResult.createError(result.getStatusCode(), result.getStatusMessage()));
  }
}
