package graphql.servlet.config;

import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.config.GraphQLProvider;

import java.util.Collection;

public interface GraphQLMutationProvider extends GraphQLProvider {
    Collection<GraphQLFieldDefinition> getMutations();
}
