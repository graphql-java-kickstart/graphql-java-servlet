package graphql.kickstart.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.cache.CachedResponse;
import graphql.kickstart.servlet.cache.GraphQLResponseCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class ErrorQueryResponseWriter implements QueryResponseWriter {

  private final int statusCode;
  private final String message;
  private final GraphQLInvocationInput invocationInput;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response, GraphQLResponseCache responseCache) throws IOException {
    if (responseCache != null) {
      try {
        responseCache.cacheResponse(request, invocationInput, CachedResponse.ofError(statusCode, message));
      } catch (Throwable t) {
        log.warn(t.getMessage(), t);
        log.warn("Ignore read from cache, unexpected error happened");
      }
    }
    response.sendError(statusCode, message);
  }

}
