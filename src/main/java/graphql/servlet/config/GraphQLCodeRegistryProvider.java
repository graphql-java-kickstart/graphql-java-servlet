package graphql.servlet.config;

import graphql.schema.GraphQLCodeRegistry;

public interface GraphQLCodeRegistryProvider extends GraphQLProvider {
    GraphQLCodeRegistry getCodeRegistry();
}
