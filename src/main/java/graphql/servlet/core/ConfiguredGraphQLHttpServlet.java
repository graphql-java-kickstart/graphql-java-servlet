package graphql.servlet.core;

import java.util.Objects;

class ConfiguredGraphQLHttpServlet extends GraphQLHttpServlet {

    private GraphQLConfiguration configuration;

    ConfiguredGraphQLHttpServlet(GraphQLConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration is required");
    }

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return configuration;
    }

}
