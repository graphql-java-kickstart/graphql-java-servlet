package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.QueryResponseWriter;
import graphql.kickstart.servlet.QueryResponseWriterFactory;
import graphql.kickstart.servlet.QueryResponseWriterFactoryImpl;

public class CachingQueryResponseWriterFactory implements QueryResponseWriterFactory {

  private final QueryResponseWriterFactory queryResponseWriterFactory =
      new QueryResponseWriterFactoryImpl();

  @Override
  public QueryResponseWriter createWriter(
      GraphQLInvocationInput invocationInput,
      GraphQLQueryResult queryResult,
      GraphQLConfiguration configuration) {
    QueryResponseWriter writer =
        queryResponseWriterFactory.createWriter(invocationInput, queryResult, configuration);
    if (configuration.getResponseCacheManager() != null) {
      return new CachingQueryResponseWriter(
          writer, configuration.getResponseCacheManager(), invocationInput, queryResult.isError());
    }
    return writer;
  }
}
