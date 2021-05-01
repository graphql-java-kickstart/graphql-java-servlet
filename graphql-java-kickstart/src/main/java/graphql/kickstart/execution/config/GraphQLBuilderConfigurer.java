package graphql.kickstart.execution.config;

import graphql.GraphQL;

public interface GraphQLBuilderConfigurer {

  void configure(GraphQL.Builder builder);
}
