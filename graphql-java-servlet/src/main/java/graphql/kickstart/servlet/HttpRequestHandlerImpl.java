package graphql.kickstart.servlet;

import graphql.GraphQLException;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.servlet.cache.CacheResponseWriter;
import graphql.kickstart.servlet.cache.CachedResponse;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    } catch (GraphQLException e) {
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
    try {
      if (configuration.getResponseCache() != null) {
        CachedResponse cachedResponse = null;
        try {
          cachedResponse = configuration.getResponseCache().getCachedResponse(invocationInput);
        } catch (Throwable t) {
          log.warn(t.getMessage(), t);
          log.warn("Ignore read from cache, unexpected error happened");
        }

        if (cachedResponse != null) {
          CacheResponseWriter cacheResponseWriter = new CacheResponseWriter();
          cacheResponseWriter.write(request, response, cachedResponse);
          return;
        }
      }

      GraphQLQueryResult queryResult = invoke(invocationInput, request, response);

      QueryResponseWriter queryResponseWriter = QueryResponseWriter.createWriter(
              queryResult,
              configuration.getObjectMapper(),
              configuration.getSubscriptionTimeout(),
              invocationInput
      );
      queryResponseWriter.write(request, response, configuration.getResponseCache());
    } catch (Throwable t) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
      log.debug("Possibly due to exception: ", t);
    }
  }

  private GraphQLQueryResult invoke(GraphQLInvocationInput invocationInput, HttpServletRequest request,
      HttpServletResponse response) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return graphQLInvoker.query(invocationInput);
    }
    return invokeBatched((GraphQLBatchedInvocationInput) invocationInput, request, response);
  }

  private GraphQLQueryResult invokeBatched(GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    BatchInputPreProcessor preprocessor = configuration.getBatchInputPreProcessor();
    BatchInputPreProcessResult result = preprocessor.preProcessBatch(batchedInvocationInput, request, response);
    if (result.isExecutable()) {
      return graphQLInvoker.query(result.getBatchedInvocationInput());
    }

    return GraphQLQueryResult.createError(result.getStatusCode(), result.getStatusMessage());
  }

}
