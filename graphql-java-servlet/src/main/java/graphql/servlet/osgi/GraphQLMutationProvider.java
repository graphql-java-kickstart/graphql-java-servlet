package graphql.servlet.osgi;

import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

public interface GraphQLMutationProvider extends GraphQLProvider {

  Collection<GraphQLFieldDefinition> getMutations();

}
