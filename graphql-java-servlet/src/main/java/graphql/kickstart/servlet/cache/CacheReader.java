package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.HttpRequestHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheReader {

  /**
   * Response from cache if possible, if nothing in cache will not produce any response
   *
   * @return {@literal true} if response was fulfilled from cache, {@literal false} is cache not
   *     found or an error occurred while reading value from cache
   * @throws IOException if can not read value from the cache
   */
  public boolean responseFromCache(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      GraphQLResponseCacheManager cacheManager)
      throws IOException {
    try {
      CachedResponse cachedResponse = cacheManager.get(request, invocationInput);
      if (cachedResponse != null) {
        write(response, cachedResponse);
        return true;
      }
    } catch (Exception t) {
      log.warn("Ignore read from cache, unexpected error happened", t);
    }

    return false;
  }

  private void write(HttpServletResponse response, CachedResponse cachedResponse)
      throws IOException {
    if (cachedResponse.isError()) {
      response.sendError(cachedResponse.getErrorStatusCode(), cachedResponse.getErrorMessage());
    } else {
      response.setContentType(HttpRequestHandler.APPLICATION_JSON_UTF8);
      response.setStatus(HttpRequestHandler.STATUS_OK);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.setContentLength(cachedResponse.getContentBytes().length);
      response.getOutputStream().write(cachedResponse.getContentBytes());
    }
  }
}
