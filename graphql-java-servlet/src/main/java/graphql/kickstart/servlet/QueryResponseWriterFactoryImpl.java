package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.util.Objects;

public class QueryResponseWriterFactoryImpl implements QueryResponseWriterFactory {

  @Override
  public QueryResponseWriter createWriter(
      GraphQLInvocationInput invocationInput,
      GraphQLQueryResult queryResult,
      GraphQLConfiguration configuration) {
    Objects.requireNonNull(queryResult, "GraphQL query result cannot be null");

    if (queryResult.isBatched()) {
      return new BatchedQueryResponseWriter(
          queryResult.getResults(), configuration.getObjectMapper());
    }
    if (queryResult.isAsynchronous()) {
      return new SingleAsynchronousQueryResponseWriter(
          queryResult.getResult(),
          configuration.getObjectMapper(),
          configuration.getSubscriptionTimeout());
    }
    if (queryResult.isError()) {
      return new ErrorQueryResponseWriter(queryResult.getStatusCode(), queryResult.getMessage());
    }
    return new SingleQueryResponseWriter(queryResult.getResult(), configuration.getObjectMapper());
  }
}
