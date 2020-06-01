package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.HttpRequestHandlerImpl;
import graphql.kickstart.servlet.QueryResponseWriter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Slf4j
public class CachingHttpRequestHandlerImpl extends HttpRequestHandlerImpl {

  private final GraphQLConfiguration configuration;

  public CachingHttpRequestHandlerImpl(GraphQLConfiguration configuration) {
    super(configuration);
    Objects.requireNonNull(configuration.getResponseCacheManager(), "Response Cache Manager cannot be null");
    this.configuration = configuration;
  }

  @Override
  protected void execute(GraphQLInvocationInput invocationInput, HttpServletRequest request,
               HttpServletResponse response) {
    // try to return value from cache if cache exists, otherwise processed the query
    boolean returnedFromCache;

    try {
      returnedFromCache = !CacheReader.responseFromCache(
          invocationInput, request, response, configuration.getResponseCacheManager()
      );
    } catch (IOException e) {
      response.setStatus(STATUS_BAD_REQUEST);
      log.warn("unexpected error happened during response from cache", e);
      return;
    }

    if (!returnedFromCache) {
      super.execute(invocationInput, request, response);
    }
  }

  protected QueryResponseWriter createWriter(GraphQLInvocationInput invocationInput, GraphQLQueryResult queryResult) {
    return CachingQueryResponseWriter.createCacheWriter(
        queryResult,
        configuration.getObjectMapper(),
        configuration.getSubscriptionTimeout(),
        invocationInput,
        configuration.getResponseCacheManager()
    );
  }

}
