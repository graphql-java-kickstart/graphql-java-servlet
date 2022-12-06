package graphql.kickstart.servlet;

import static graphql.kickstart.servlet.HttpRequestHandler.STATUS_BAD_REQUEST;
import static graphql.kickstart.servlet.HttpRequestHandler.STATUS_INTERNAL_SERVER_ERROR;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLException;
import graphql.kickstart.execution.FutureExecutionResult;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.error.GenericGraphQLError;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
      HttpServletResponse response,
      ListenerHandler listenerHandler) {
    if (request.isAsyncSupported()) {
      invokeAndHandleAsync(invocationInput, request, response, listenerHandler);
    } else {
      handle(invocationInput, request, response, listenerHandler);
    }
  }

  private void invokeAndHandleAsync(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler) {
    AsyncContext asyncContext =
        request.isAsyncStarted()
            ? request.getAsyncContext()
            : request.startAsync(request, response);
    asyncContext.setTimeout(configuration.getAsyncTimeout());
    AtomicReference<FutureExecutionResult> futureHolder = new AtomicReference<>();
    AsyncTimeoutListener timeoutListener =
        event -> {
          log.warn(
              "GraphQL execution canceled because timeout of "
                  + configuration.getAsyncTimeout()
                  + " millis was reached. The following query was being executed when this happened:\n{}",
              String.join("\n", invocationInput.getQueries()));
          FutureExecutionResult futureResult = futureHolder.get();
          if (futureResult != null) {
            futureResult.cancel();
          } else {
            writeErrorResponse(
                invocationInput, request, response, listenerHandler, new CancellationException());
          }
        };
    asyncContext.addListener(timeoutListener);
    configuration
        .getAsyncExecutor()
        .execute(
            () -> {
              try {
                FutureExecutionResult futureResult = invoke(invocationInput, request, response);
                futureHolder.set(futureResult);
                handleInternal(futureResult, request, response, listenerHandler)
                    .thenAccept(it -> asyncContext.complete());
              } catch (GraphQLException e) {
                response.setStatus(STATUS_BAD_REQUEST);
                log.info("Bad request: cannot handle http request", e);
                listenerHandler.onError(e);
                asyncContext.complete();
              } catch (Exception e) {
                response.setStatus(STATUS_INTERNAL_SERVER_ERROR);
                log.error("Cannot handle http request", e);
                listenerHandler.onError(e);
                asyncContext.complete();
              }
            });
  }

  private void handle(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler) {
    try {
      FutureExecutionResult futureResult = invoke(invocationInput, request, response);
      handleInternal(futureResult, request, response, listenerHandler)
          .get(configuration.getAsyncTimeout(), TimeUnit.MILLISECONDS);
    } catch (GraphQLException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad request: cannot handle http request", e);
      listenerHandler.onError(e);
    } catch (Exception e) {
      response.setStatus(STATUS_INTERNAL_SERVER_ERROR);
      log.error("Cannot handle http request", e);
      listenerHandler.onError(e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private CompletableFuture<Void> handleInternal(
      FutureExecutionResult futureResult,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler) {
    return futureResult
        .thenApplyQueryResult()
        .thenAccept(
            it -> {
              listenerHandler.beforeFlush();
              writeResultResponse(futureResult.getInvocationInput(), it, request, response);
            })
        .thenAccept(it -> listenerHandler.onSuccess())
        .exceptionally(
            t ->
                writeErrorResponse(
                    futureResult.getInvocationInput(), request, response, listenerHandler, t))
        .thenAccept(it -> listenerHandler.onFinally());
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

  private Void writeErrorResponse(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler,
      Throwable t) {
    Throwable cause = getCause(t);
    if (!response.isCommitted()) {
      writeResultResponse(
          invocationInput, GraphQLQueryResult.create(toErrorResult(cause)), request, response);
      listenerHandler.onError(cause);
    } else {
      log.warn(
          "Cannot write GraphQL response, because the HTTP response is already committed. It most likely timed out.");
    }
    return null;
  }

  private Throwable getCause(Throwable t) {
    return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
  }

  private ExecutionResult toErrorResult(Throwable t) {
    String message =
        t instanceof CancellationException
            ? "Execution canceled because timeout of "
                + configuration.getAsyncTimeout()
                + " millis was reached"
            : t.getMessage();
    if (message == null) {
      message = "Unexpected error occurred";
    }
    return new ExecutionResultImpl(new GenericGraphQLError(message));
  }

  protected QueryResponseWriter createWriter(
      GraphQLInvocationInput invocationInput, GraphQLQueryResult queryResult) {
    return queryResponseWriterFactory.createWriter(invocationInput, queryResult, configuration);
  }

  private FutureExecutionResult invoke(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return graphQLInvoker.execute(invocationInput);
    }
    return invokeBatched((GraphQLBatchedInvocationInput) invocationInput, request, response);
  }

  private FutureExecutionResult invokeBatched(
      GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    BatchInputPreProcessor preprocessor = configuration.getBatchInputPreProcessor();
    BatchInputPreProcessResult result =
        preprocessor.preProcessBatch(batchedInvocationInput, request, response);
    if (result.isExecutable()) {
      return graphQLInvoker.execute(result.getBatchedInvocationInput());
    }

    return FutureExecutionResult.error(
        GraphQLQueryResult.createError(result.getStatusCode(), result.getStatusMessage()));
  }
}
