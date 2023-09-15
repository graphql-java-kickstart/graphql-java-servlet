package graphql.kickstart.servlet.osgi;

import graphql.kickstart.servlet.GraphQLConfiguration;

public interface GraphQLConfigurationProvider extends GraphQLProvider {

  GraphQLConfiguration.Builder getConfigurationBuilder();
}
