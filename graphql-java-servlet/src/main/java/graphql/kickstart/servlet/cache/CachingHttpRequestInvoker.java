package graphql.kickstart.servlet.cache;

import static graphql.kickstart.servlet.HttpRequestHandler.STATUS_BAD_REQUEST;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.HttpRequestInvoker;
import graphql.kickstart.servlet.HttpRequestInvokerImpl;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CachingHttpRequestInvoker implements HttpRequestInvoker {

  private final GraphQLConfiguration configuration;
  private final HttpRequestInvoker requestInvoker;

  public CachingHttpRequestInvoker(GraphQLConfiguration configuration) {
    this.configuration = configuration;
    requestInvoker = new HttpRequestInvokerImpl(configuration, configuration.getGraphQLInvoker(),
        new CachingQueryResponseWriterFactory());
  }

  @Override
  public void execute(GraphQLInvocationInput invocationInput, HttpServletRequest request,
      HttpServletResponse response) {
    // try to return value from cache if cache exists, otherwise processed the query
    boolean returnedFromCache;

    try {
      returnedFromCache = !CacheReader.responseFromCache(
          invocationInput, request, response, configuration.getResponseCacheManager()
      );
    } catch (IOException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.warn("Unexpected error happened during response from cache", e);
      return;
    }

    if (!returnedFromCache) {
      requestInvoker.execute(invocationInput, request, response);
    }
  }

}
