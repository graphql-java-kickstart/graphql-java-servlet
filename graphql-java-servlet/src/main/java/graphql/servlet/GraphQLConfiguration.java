package graphql.servlet;

import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GraphQLConfiguration {

    private GraphQLInvocationInputFactory invocationInputFactory;
    private GraphQLQueryInvoker queryInvoker;
    private GraphQLObjectMapper objectMapper;
    private List<GraphQLServletListener> listeners;
    private boolean asyncServletModeEnabled;

    public static GraphQLConfiguration.Builder with(GraphQLSchema schema) {
        return with(new DefaultGraphQLSchemaProvider(schema));
    }

    public static GraphQLConfiguration.Builder with(GraphQLSchemaProvider schemaProvider) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schemaProvider));
    }

    static GraphQLConfiguration.Builder with(GraphQLInvocationInputFactory invocationInputFactory) {
        return new Builder(invocationInputFactory);
    }

    private GraphQLConfiguration(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper objectMapper, List<GraphQLServletListener> listeners, boolean asyncServletModeEnabled) {
        this.invocationInputFactory = invocationInputFactory;
        this.queryInvoker = queryInvoker;
        this.objectMapper = objectMapper;
        this.listeners = listeners;
        this.asyncServletModeEnabled = asyncServletModeEnabled;
    }

    public GraphQLInvocationInputFactory getInvocationInputFactory() {
        return invocationInputFactory;
    }

    public GraphQLQueryInvoker getQueryInvoker() {
        return queryInvoker;
    }

    public GraphQLObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public List<GraphQLServletListener> getListeners() {
        return new ArrayList<>(listeners);
    }

    public boolean isAsyncServletModeEnabled() {
        return asyncServletModeEnabled;
    }

    public void add(GraphQLServletListener listener) {
        listeners.add(listener);
    }

    public boolean remove(GraphQLServletListener listener) {
        return listeners.remove(listener);
    }

    public static class Builder {

        private GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder;
        private GraphQLInvocationInputFactory invocationInputFactory;
        private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
        private GraphQLObjectMapper objectMapper = GraphQLObjectMapper.newBuilder().build();
        private List<GraphQLServletListener> listeners = new ArrayList<>();
        private boolean asyncServletModeEnabled = false;

        private Builder(GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder) {
            this.invocationInputFactoryBuilder = invocationInputFactoryBuilder;
        }

        private Builder(GraphQLInvocationInputFactory invocationInputFactory) {
            this.invocationInputFactory = invocationInputFactory;
        }

        public Builder with(GraphQLQueryInvoker queryInvoker) {
            this.queryInvoker = queryInvoker;
            return this;
        }

        public Builder with(GraphQLObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder with(List<GraphQLServletListener> listeners) {
            this.listeners = Objects.requireNonNull(listeners, "listeners must not be null");
            return this;
        }

        public Builder with(boolean asyncServletModeEnabled) {
            this.asyncServletModeEnabled = asyncServletModeEnabled;
            return this;
        }

        public GraphQLConfiguration build() {
            return new GraphQLConfiguration(
                    this.invocationInputFactory != null ? this.invocationInputFactory : invocationInputFactoryBuilder.build(),
                    queryInvoker,
                    objectMapper,
                    listeners,
                    asyncServletModeEnabled
            );
        }

    }

}
