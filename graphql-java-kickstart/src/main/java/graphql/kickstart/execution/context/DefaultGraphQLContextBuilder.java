package graphql.kickstart.execution.context;

/** Returns an empty context. */
public class DefaultGraphQLContextBuilder implements GraphQLContextBuilder {

  @Override
  public GraphQLKickstartContext build() {
    return new DefaultGraphQLContext();
  }
}
