package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.QueryResponseWriter;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CachingQueryResponseWriter implements QueryResponseWriter {

  private final QueryResponseWriter delegate;
  private final GraphQLResponseCacheManager responseCache;
  private final GraphQLInvocationInput invocationInput;
  private final boolean error;

  public CachingQueryResponseWriter(
      QueryResponseWriter delegate,
      GraphQLResponseCacheManager responseCache,
      GraphQLInvocationInput invocationInput,
      boolean error) {
    this.delegate = delegate;
    this.responseCache = responseCache;
    this.invocationInput = invocationInput;
    this.error = error;
  }

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (responseCache.isCacheable(request, invocationInput)) {
      BufferedHttpServletResponse cachingResponseWrapper =
          new BufferedHttpServletResponse(response);

      delegate.write(request, cachingResponseWrapper);

      try {
        if (error) {
          int errorStatusCode = cachingResponseWrapper.getStatus();
          String errorMessage = cachingResponseWrapper.getErrorMessage();

          responseCache.put(
              request, invocationInput, CachedResponse.ofError(errorStatusCode, errorMessage));
        } else {
          byte[] contentBytes = cachingResponseWrapper.getContentAsByteArray();

          responseCache.put(request, invocationInput, CachedResponse.ofContent(contentBytes));
        }
      } catch (Exception t) {
        log.warn("Ignore read from cache, unexpected error happened", t);
      }

      cachingResponseWrapper.flushBuffer();
      cachingResponseWrapper.close();
    } else {
      delegate.write(request, response);
    }
  }
}
