package graphql.kickstart.execution.context;

public interface GraphQLContextBuilder {

  /** @return the graphql context */
  GraphQLKickstartContext build();
}
