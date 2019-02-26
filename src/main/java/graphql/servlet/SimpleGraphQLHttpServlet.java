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

    /**
     * @deprecated use {@link GraphQLHttpServlet} instead
     */
    @Deprecated
    public SimpleGraphQLHttpServlet(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper, List<GraphQLServletListener> listeners, boolean asyncServletMode) {
        super(listeners);
        this.configuration = GraphQLConfiguration.with(invocationInputFactory)
                .with(queryInvoker)
                .with(graphQLObjectMapper)
                .with(listeners != null ? listeners : new ArrayList<>())
                .with(asyncServletMode)
                .build();
    }

    /**
     * @deprecated use {@link GraphQLHttpServlet} instead
     */
    @Deprecated
    public SimpleGraphQLHttpServlet(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper, List<GraphQLServletListener> listeners, boolean asyncServletMode, long subscriptionTimeout) {
        super(listeners);
        this.configuration = GraphQLConfiguration.with(invocationInputFactory)
                .with(queryInvoker)
                .with(graphQLObjectMapper)
                .with(listeners != null ? listeners : new ArrayList<>())
                .with(asyncServletMode)
                .with(subscriptionTimeout)
                .build();
    }

    private SimpleGraphQLHttpServlet(GraphQLConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration is required");
    }

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        return configuration.getQueryInvoker();
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        return configuration.getInvocationInputFactory();
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        return configuration.getObjectMapper();
    }

    @Override
    protected GraphQLBatchExecutionHandlerFactory getBatchExecutionHandlerFactory() {
        return configuration.getBatchExecutionHandlerFactory();
    }

    @Override
    protected boolean isAsyncServletMode() {
        return configuration.isAsyncServletModeEnabled();
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
        private long subscriptionTimeout;

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

        public Builder withSubscriptionTimeout(long subscriptionTimeout) {
            this.subscriptionTimeout = subscriptionTimeout;
            return this;
        }

        @Deprecated
        public SimpleGraphQLHttpServlet build() {
            GraphQLConfiguration configuration = GraphQLConfiguration.with(invocationInputFactory)
                    .with(queryInvoker)
                    .with(graphQLObjectMapper)
                    .with(listeners != null ? listeners : new ArrayList<>())
                    .with(asyncServletMode)
                    .with(subscriptionTimeout)
                    .build();
            return new SimpleGraphQLHttpServlet(configuration);
        }
    }
}
