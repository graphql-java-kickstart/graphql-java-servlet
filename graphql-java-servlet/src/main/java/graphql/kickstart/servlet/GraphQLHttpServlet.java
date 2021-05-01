package graphql.kickstart.servlet;

import graphql.schema.GraphQLSchema;

/** @author Michiel Oliemans */
public abstract class GraphQLHttpServlet extends AbstractGraphQLHttpServlet {

  public static GraphQLHttpServlet with(GraphQLSchema schema) {
    return new ConfiguredGraphQLHttpServlet(GraphQLConfiguration.with(schema).build());
  }

  public static GraphQLHttpServlet with(GraphQLConfiguration configuration) {
    return new ConfiguredGraphQLHttpServlet(configuration);
  }

  @Override
  protected abstract GraphQLConfiguration getConfiguration();
}
