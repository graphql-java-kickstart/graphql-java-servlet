package graphql.kickstart.execution.context;

/**
 * Returns an empty context.
 */
public class DefaultGraphQLContextBuilder implements GraphQLContextBuilder {

  @Override
  public GraphQLContext build() {
    return new DefaultGraphQLContext();
  }

}
