package graphql.kickstart.execution.config;

import graphql.schema.GraphQLCodeRegistry;
import graphql.servlet.osgi.GraphQLProvider;

public interface GraphQLCodeRegistryProvider extends GraphQLProvider {
    GraphQLCodeRegistry getCodeRegistry();
}
