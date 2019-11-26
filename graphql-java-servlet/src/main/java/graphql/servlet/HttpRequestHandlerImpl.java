package graphql.servlet;

import static graphql.servlet.QueryResponseWriter.createWriter;

import graphql.GraphQLException;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.servlet.input.BatchInputPreProcessResult;
import graphql.servlet.input.BatchInputPreProcessor;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.io.IOException;
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
      GraphQLQueryResult queryResult = invoke(invocationInput, request, response);

      QueryResponseWriter queryResponseWriter = createWriter(queryResult, configuration.getObjectMapper(),
          configuration.getSubscriptionTimeout());
      queryResponseWriter.write(request, response);
    } catch (Throwable t) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
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
