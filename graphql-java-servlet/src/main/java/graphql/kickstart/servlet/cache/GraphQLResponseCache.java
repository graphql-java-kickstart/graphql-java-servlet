package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.input.GraphQLInvocationInput;

import java.util.Optional;

public interface GraphQLResponseCache {

  /**
   * Retrieve the cache by input data. If this query was not cached before, will return empty {@link Optional}.
   *
   * @param invocationInput input data
   * @return cached response if something available in cache or {@literal null} if nothing cached
   */
  CachedResponse getCachedResponse(GraphQLInvocationInput invocationInput);

  /**
   * Decide to cache or not this response. It depends on the implementation.
   *
   * @param invocationInput input data
   * @param cachedResponse  response to cache
   */
  void cacheResponse(GraphQLInvocationInput invocationInput, CachedResponse cachedResponse);

}
