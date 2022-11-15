package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;

public interface GraphQLResponseCacheManager {

  /**
   * Retrieve the cache by input data. If this query was not cached before, will return empty {@link
   * Optional}.
   *
   * @param request the http request
   * @param invocationInput input data
   * @return cached response if something available in cache or {@literal null} if nothing cached
   */
  CachedResponse get(HttpServletRequest request, GraphQLInvocationInput invocationInput);

  /**
   * Decide to cache or not this response. It depends on the implementation.
   *
   * @param request the http request
   * @param invocationInput input data
   */
  boolean isCacheable(HttpServletRequest request, GraphQLInvocationInput invocationInput);

  /**
   * Cache this response. It depends on the implementation.
   *
   * @param request the http request
   * @param invocationInput input data
   * @param cachedResponse response to cache
   */
  void put(
      HttpServletRequest request,
      GraphQLInvocationInput invocationInput,
      CachedResponse cachedResponse);
}
