package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.cache.GraphQLResponseCache;

import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

interface QueryResponseWriter {

  static QueryResponseWriter createWriter(
      GraphQLQueryResult result,
      GraphQLObjectMapper graphQLObjectMapper,
      long subscriptionTimeout,
      GraphQLInvocationInput invocationInput
  ) {
    Objects.requireNonNull(result, "GraphQL query result cannot be null");

    if (result.isBatched()) {
      return new BatchedQueryResponseWriter(result.getResults(), graphQLObjectMapper, invocationInput);
    } else if (result.isAsynchronous()) {
      return new SingleAsynchronousQueryResponseWriter(result.getResult(), graphQLObjectMapper, subscriptionTimeout, invocationInput);
    } else if (result.isError()) {
      return new ErrorQueryResponseWriter(result.getStatusCode(), result.getMessage(), invocationInput);
    }
    return new SingleQueryResponseWriter(result.getResult(), graphQLObjectMapper, invocationInput);
  }

  void write(HttpServletRequest request, HttpServletResponse response, GraphQLResponseCache responseCache) throws IOException;

}
