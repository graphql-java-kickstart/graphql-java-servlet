package graphql.kickstart.servlet;

import graphql.GraphQLException;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class HttpRequestHandlerImpl implements HttpRequestHandler {

  private final GraphQLConfiguration configuration;
  private final HttpRequestInvoker requestInvoker;

  HttpRequestHandlerImpl(GraphQLConfiguration configuration) {
    this(
        configuration,
        new HttpRequestInvokerImpl(
            configuration,
            configuration.getGraphQLInvoker(),
            new QueryResponseWriterFactoryImpl()));
  }

  HttpRequestHandlerImpl(
      GraphQLConfiguration configuration, HttpRequestInvoker requestInvoker) {
    this.configuration = configuration;
    this.requestInvoker = requestInvoker;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (request.getCharacterEncoding() == null) {
      request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    ListenerHandler listenerHandler =
      ListenerHandler.start(request, response, configuration.getListeners());

    try {
      GraphQLInvocationInput invocationInput = parseInvocationInput(request, response);
      requestInvoker.execute(invocationInput, request, response, listenerHandler);
    } catch (InvocationInputParseException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad request: cannot parse http request", e);
      listenerHandler.onParseError(e);
      throw e;
    } catch (GraphQLException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.info("Bad request: cannot handle http request", e);
      throw e;
    } catch (Exception t) {
      response.setStatus(STATUS_INTERNAL_SERVER_ERROR);
      log.error("Cannot handle http request", t);
      throw t;
    }
  }

  private GraphQLInvocationInput parseInvocationInput(
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      GraphQLInvocationInputParser invocationInputParser =
        GraphQLInvocationInputParser.create(
          request,
          configuration.getInvocationInputFactory(),
          configuration.getObjectMapper(),
          configuration.getContextSetting());
      return invocationInputParser.getGraphQLInvocationInput(request, response);
    } catch (Exception e) {
      throw new InvocationInputParseException(e);
    }
  }
}
