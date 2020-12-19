package graphql.kickstart.servlet.cache;

import graphql.kickstart.execution.GraphQLQueryResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.QueryResponseWriter;
import graphql.kickstart.servlet.QueryResponseWriterFactory;

public class CachingQueryResponseWriterFactory implements QueryResponseWriterFactory {

  @Override
  public QueryResponseWriter createWriter(GraphQLInvocationInput invocationInput,
      GraphQLQueryResult queryResult, GraphQLConfiguration configuration) {
    QueryResponseWriter writer = QueryResponseWriter
        .createWriter(queryResult, configuration.getObjectMapper(),
            configuration.getSubscriptionTimeout());
    if (configuration.getResponseCacheManager() != null) {
      return new CachingQueryResponseWriter(writer, configuration.getResponseCacheManager(),
          invocationInput,
          queryResult.isError());
    }
    return writer;
  }
}
