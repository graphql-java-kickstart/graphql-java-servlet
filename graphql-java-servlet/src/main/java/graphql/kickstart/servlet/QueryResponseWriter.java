package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.cache.CachingQueryResponseWriter;
import graphql.kickstart.servlet.cache.GraphQLResponseCacheManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public interface QueryResponseWriter {

  static QueryResponseWriter createWriter(
      GraphQLQueryResult result,
      GraphQLObjectMapper graphQLObjectMapper,
      long subscriptionTimeout
  ) {
    Objects.requireNonNull(result, "GraphQL query result cannot be null");

    if (result.isBatched()) {
      return new BatchedQueryResponseWriter(result.getResults(), graphQLObjectMapper);
    } else if (result.isAsynchronous()) {
      return new SingleAsynchronousQueryResponseWriter(result.getResult(), graphQLObjectMapper, subscriptionTimeout);
    } else if (result.isError()) {
      return new ErrorQueryResponseWriter(result.getStatusCode(), result.getMessage());
    }
    return new SingleQueryResponseWriter(result.getResult(), graphQLObjectMapper);
  }

  static QueryResponseWriter createCacheWriter(
      GraphQLQueryResult result,
      GraphQLObjectMapper graphQLObjectMapper,
      long subscriptionTimeout,
      GraphQLInvocationInput invocationInput,
      GraphQLResponseCacheManager responseCache
  ) {
    QueryResponseWriter writer = createWriter(result, graphQLObjectMapper, subscriptionTimeout);
    if (responseCache != null) {
      return new CachingQueryResponseWriter(writer, responseCache, invocationInput, result.isError());
    }
    return writer;
  }

  void write(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
