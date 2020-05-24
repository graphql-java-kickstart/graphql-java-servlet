package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.cache.CachingQueryResponseWriter;
import graphql.kickstart.servlet.cache.GraphQLResponseCacheManager;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface QueryResponseWriter {

  static QueryResponseWriter createWriter(
      GraphQLQueryResult result,
      GraphQLObjectMapper graphQLObjectMapper,
      long subscriptionTimeout,
      GraphQLInvocationInput invocationInput,
      GraphQLResponseCacheManager responseCache
  ) {
    Objects.requireNonNull(result, "GraphQL query result cannot be null");

    QueryResponseWriter writer;

    if (result.isBatched()) {
      writer = new BatchedQueryResponseWriter(result.getResults(), graphQLObjectMapper);
    } else if (result.isAsynchronous()) {
      writer = new SingleAsynchronousQueryResponseWriter(result.getResult(), graphQLObjectMapper, subscriptionTimeout);
    } else if (result.isError()) {
      writer = new ErrorQueryResponseWriter(result.getStatusCode(), result.getMessage());
    } else {
      writer = new SingleQueryResponseWriter(result.getResult(), graphQLObjectMapper);
    }

    if (responseCache != null) {
      writer = new CachingQueryResponseWriter(writer, responseCache, invocationInput, result.isError());
    }
    return writer;
  }

  void write(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
