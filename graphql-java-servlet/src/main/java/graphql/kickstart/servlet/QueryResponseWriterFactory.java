package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;

public interface QueryResponseWriterFactory {

  QueryResponseWriter createWriter(
      GraphQLInvocationInput invocationInput,
      GraphQLQueryResult queryResult,
      GraphQLConfiguration configuration);
}
