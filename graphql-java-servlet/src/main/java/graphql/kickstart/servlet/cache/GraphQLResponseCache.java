package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.input.GraphQLInvocationInput;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface GraphQLResponseCache {

  /**
   * Retrieve the cache by input data. If this query was not cached before, will return empty {@link Optional}.
   *
   * @param invocationInput input data
   * @return cached response if something available in cache or {@literal null} if nothing cached
   */
  CachedResponse getCachedResponse(HttpServletRequest request, GraphQLInvocationInput invocationInput);

  /**
   * Decide to cache or not this response. It depends on the implementation.
   *
   * @param invocationInput input data
   * @param cachedResponse  response to cache
   */
  void cacheResponse(HttpServletRequest request, GraphQLInvocationInput invocationInput, CachedResponse cachedResponse);

}
