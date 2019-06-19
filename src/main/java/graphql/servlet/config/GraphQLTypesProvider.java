package graphql.servlet.config;

import graphql.schema.GraphQLType;
import graphql.servlet.config.GraphQLProvider;

import java.util.Collection;

public interface GraphQLTypesProvider extends GraphQLProvider {
    Collection<GraphQLType> getTypes();
}
