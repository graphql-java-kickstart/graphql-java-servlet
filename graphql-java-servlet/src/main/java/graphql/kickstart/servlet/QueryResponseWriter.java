package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryResult;
import java.io.IOException;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
      return new SingleAsynchronousQueryResponseWriter(result.getResult(), graphQLObjectMapper,
          subscriptionTimeout);
    } else if (result.isError()) {
      return new ErrorQueryResponseWriter(result.getStatusCode(), result.getMessage());
    }
    return new SingleQueryResponseWriter(result.getResult(), graphQLObjectMapper);
  }

  void write(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
