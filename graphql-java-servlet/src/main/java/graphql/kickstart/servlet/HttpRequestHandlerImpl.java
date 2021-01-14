package graphql.kickstart.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.GraphQLException;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class HttpRequestHandlerImpl implements HttpRequestHandler {

  private final GraphQLConfiguration configuration;
  private final HttpRequestInvoker requestInvoker;

  public HttpRequestHandlerImpl(GraphQLConfiguration configuration) {
    this(configuration, new HttpRequestInvokerImpl(configuration, configuration.getGraphQLInvoker(),
        new QueryResponseWriterFactoryImpl()));
  }

  public HttpRequestHandlerImpl(GraphQLConfiguration configuration,
      HttpRequestInvoker requestInvoker) {
    this.configuration = configuration;
    this.requestInvoker = requestInvoker;
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
      GraphQLInvocationInput invocationInput = invocationInputParser
          .getGraphQLInvocationInput(request, response);
      requestInvoker.execute(invocationInput, request, response);
    } catch (GraphQLException | JsonProcessingException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad request: cannot handle http request", e);
      throw e;
    } catch (Exception t) {
      response.setStatus(STATUS_INTERNAL_SERVER_ERROR);
      log.error("Cannot handle http request", t);
      throw t;
    }
  }

}
