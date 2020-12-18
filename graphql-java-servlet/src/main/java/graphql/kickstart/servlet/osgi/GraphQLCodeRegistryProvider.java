package graphql.kickstart.servlet.osgi;

import graphql.schema.GraphQLCodeRegistry;

public interface GraphQLCodeRegistryProvider extends GraphQLProvider {

  GraphQLCodeRegistry getCodeRegistry();
}
