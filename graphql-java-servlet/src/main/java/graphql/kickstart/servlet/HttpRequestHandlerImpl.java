package graphql.kickstart.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.GraphQLException;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class HttpRequestHandlerImpl implements HttpRequestHandler {

  private final GraphQLConfiguration configuration;
  private final GraphQLInvoker graphQLInvoker;

  HttpRequestHandlerImpl(GraphQLConfiguration configuration) {
    this.configuration = configuration;
    graphQLInvoker = configuration.getGraphQLInvoker();
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      GraphQLInvocationInputParser invocationInputParser = GraphQLInvocationInputParser.create(
          request,
          configuration.getInvocationInputFactory(),
          configuration.getObjectMapper(),
          configuration.getContextSetting()
      );
      GraphQLInvocationInput invocationInput = invocationInputParser.getGraphQLInvocationInput(request, response);
      execute(invocationInput, request, response);
    } catch (GraphQLException| JsonProcessingException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad request: cannot create invocation input parser", e);
      throw e;
    } catch (Throwable t) {
      response.setStatus(500);
      log.info("Bad request: cannot create invocation input parser", t);
      throw t;
    }
  }

  private void execute(GraphQLInvocationInput invocationInput, HttpServletRequest request,
      HttpServletResponse response) {
    if (request.isAsyncSupported()) {
      AsyncContext asyncContext = request.isAsyncStarted()
          ? request.getAsyncContext()
          : request.startAsync(request, response);
      asyncContext.setTimeout(configuration.getAsyncTimeout());
      invoke(invocationInput, request, response)
          .thenAccept(result -> writeResultResponse(result, request, response))
          .exceptionally(t -> writeErrorResponse(t, response))
          .thenAccept(aVoid -> asyncContext.complete());
    } else {
      try {
        GraphQLQueryResult result = invoke(invocationInput, request, response).join();
        writeResultResponse(result, request, response);
      } catch (Throwable t) {
        writeErrorResponse(t, response);
      }
    }
  }

  private void writeResultResponse(GraphQLQueryResult queryResult, HttpServletRequest request,
      HttpServletResponse response) {
    QueryResponseWriter queryResponseWriter = QueryResponseWriter.createWriter(queryResult, configuration.getObjectMapper(),
        configuration.getSubscriptionTimeout());
    try {
      queryResponseWriter.write(request, response);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Void writeErrorResponse(Throwable t, HttpServletResponse response) {
    response.setStatus(STATUS_BAD_REQUEST);
    log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given", t);
    return null;
  }

  private CompletableFuture<GraphQLQueryResult> invoke(GraphQLInvocationInput invocationInput, HttpServletRequest request,
      HttpServletResponse response) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return graphQLInvoker.queryAsync(invocationInput);
    }
    return invokeBatched((GraphQLBatchedInvocationInput) invocationInput, request, response);
  }

  private CompletableFuture<GraphQLQueryResult> invokeBatched(GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    BatchInputPreProcessor preprocessor = configuration.getBatchInputPreProcessor();
    BatchInputPreProcessResult result = preprocessor.preProcessBatch(batchedInvocationInput, request, response);
    if (result.isExecutable()) {
      return graphQLInvoker.queryAsync(result.getBatchedInvocationInput());
    }

    return CompletableFuture.completedFuture(GraphQLQueryResult.createError(result.getStatusCode(), result.getStatusMessage()));
  }

}
