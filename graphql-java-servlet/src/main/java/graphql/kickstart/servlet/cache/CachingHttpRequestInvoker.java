package graphql.kickstart.servlet.cache;

import static graphql.kickstart.servlet.HttpRequestHandler.STATUS_BAD_REQUEST;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.HttpRequestInvoker;
import graphql.kickstart.servlet.HttpRequestInvokerImpl;
import graphql.kickstart.servlet.ListenerHandler;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CachingHttpRequestInvoker implements HttpRequestInvoker {

  private final GraphQLConfiguration configuration;
  private final HttpRequestInvoker requestInvoker;
  private final CacheReader cacheReader;

  public CachingHttpRequestInvoker(GraphQLConfiguration configuration) {
    this(
        configuration,
        new HttpRequestInvokerImpl(
            configuration,
            configuration.getGraphQLInvoker(),
            new CachingQueryResponseWriterFactory()),
        new CacheReader());
  }

  /** Try to return value from cache if cache exists, otherwise process the query normally */
  @Override
  public void execute(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler) {
    try {
      if (!cacheReader.responseFromCache(
          invocationInput, request, response, configuration.getResponseCacheManager())) {
        requestInvoker.execute(invocationInput, request, response, listenerHandler);
      }
    } catch (IOException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.warn("Unexpected error happened during response from cache", e);
    }
  }
}
