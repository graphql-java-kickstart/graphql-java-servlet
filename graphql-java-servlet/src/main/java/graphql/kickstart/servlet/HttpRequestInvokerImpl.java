package graphql.kickstart.servlet;

import static graphql.kickstart.servlet.HttpRequestHandler.STATUS_BAD_REQUEST;

import graphql.ExecutionResultImpl;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
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
    ListenerHandler listenerHandler =
        ListenerHandler.start(request, response, configuration.getListeners());
    if (request.isAsyncSupported()) {
      invokeAndHandleAsync(invocationInput, request, response, listenerHandler);
    } else {
      invokeAndHandle(invocationInput, request, response, listenerHandler).join();
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
    AtomicReference<CompletableFuture<Void>> futureHolder = new AtomicReference<>();
    AsyncTimeoutListener timeoutListener =
        event -> {
          Optional.ofNullable(futureHolder.get()).ifPresent(it -> it.cancel(true));
          writeResultResponse(
              invocationInput,
              GraphQLQueryResult.create(
                  new ExecutionResultImpl(new GenericGraphQLError("Timeout"))),
              (HttpServletRequest) event.getAsyncContext().getRequest(),
              (HttpServletResponse) event.getAsyncContext().getResponse());
          listenerHandler.onError(event.getThrowable());
        };
    asyncContext.addListener(timeoutListener);
    asyncContext.start(
        () ->
            futureHolder.set(
                invokeAndHandle(invocationInput, request, response, listenerHandler)
                    .thenAccept(aVoid -> asyncContext.complete())));
  }

  private CompletableFuture<Void> invokeAndHandle(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler) {
    return invoke(invocationInput, request, response)
        .thenAccept(it -> writeResultResponse(invocationInput, it, request, response))
        .thenAccept(it -> listenerHandler.onSuccess())
        .exceptionally(t -> writeBadRequestError(t, response, listenerHandler))
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

  protected QueryResponseWriter createWriter(
      GraphQLInvocationInput invocationInput, GraphQLQueryResult queryResult) {
    return queryResponseWriterFactory.createWriter(invocationInput, queryResult, configuration);
  }

  private Void writeBadRequestError(
      Throwable t, HttpServletResponse response, ListenerHandler listenerHandler) {
    if (!response.isCommitted()) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info(
          "Bad request: path was not \"/schema.json\" or no query variable named \"query\" given",
          t);
      listenerHandler.onError(t);
    } else {
      log.warn(
          "Cannot write GraphQL response, because the HTTP response is already committed. It most likely timed out.");
    }
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
