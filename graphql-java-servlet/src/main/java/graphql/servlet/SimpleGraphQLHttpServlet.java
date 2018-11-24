package graphql.servlet;

import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Andrew Potter
 */
public class SimpleGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    private GraphQLConfiguration configuration;

    public SimpleGraphQLHttpServlet() {
    }

    private SimpleGraphQLHttpServlet(GraphQLConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration is required");
    }

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return configuration;
    }

    public static Builder newBuilder(GraphQLSchema schema) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schema).build());
    }

    public static Builder newBuilder(GraphQLSchemaProvider schemaProvider) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schemaProvider).build());
    }

    public static Builder newBuilder(GraphQLInvocationInputFactory invocationInputFactory) {
        return new Builder(invocationInputFactory);
    }

    public static class Builder {
        private final GraphQLInvocationInputFactory invocationInputFactory;
        private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
        private GraphQLObjectMapper graphQLObjectMapper = GraphQLObjectMapper.newBuilder().build();
        private List<GraphQLServletListener> listeners;
        private boolean asyncServletMode;

        Builder(GraphQLInvocationInputFactory invocationInputFactory) {
            this.invocationInputFactory = invocationInputFactory;
        }

        public Builder withQueryInvoker(GraphQLQueryInvoker queryInvoker) {
            this.queryInvoker = queryInvoker;
            return this;
        }

        public Builder withObjectMapper(GraphQLObjectMapper objectMapper) {
            this.graphQLObjectMapper = objectMapper;
            return this;
        }

        public Builder withAsyncServletMode(boolean asyncServletMode) {
            this.asyncServletMode = asyncServletMode;
            return this;
        }

        public Builder withListeners(List<GraphQLServletListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        @Deprecated
        public SimpleGraphQLHttpServlet build() {
            GraphQLConfiguration configuration = GraphQLConfiguration.with(invocationInputFactory)
                    .with(queryInvoker)
                    .with(graphQLObjectMapper)
                    .with(listeners != null ? listeners : new ArrayList<>())
                    .with(asyncServletMode)
                    .build();
            return new SimpleGraphQLHttpServlet(configuration);
        }
    }
}
