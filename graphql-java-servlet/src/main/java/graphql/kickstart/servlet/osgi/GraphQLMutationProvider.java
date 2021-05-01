package graphql.kickstart.servlet.osgi;

import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

public interface GraphQLMutationProvider extends GraphQLFieldProvider {

  Collection<GraphQLFieldDefinition> getMutations();

  default Collection<GraphQLFieldDefinition> getFields() {
    return getMutations();
  }
}
