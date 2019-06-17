package graphql.servlet.core;

import graphql.schema.GraphQLSchema;
import graphql.servlet.DefaultGraphQLSchemaProvider;
import graphql.servlet.GraphQLContextBuilder;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;
import graphql.servlet.GraphQLRootObjectBuilder;
import graphql.servlet.GraphQLSchemaProvider;
import graphql.servlet.GraphQLServletListener;
import graphql.servlet.input.ContextSetting;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import graphql.servlet.internal.GraphQLThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GraphQLConfiguration {

    private GraphQLInvocationInputFactory invocationInputFactory;
    private GraphQLQueryInvoker queryInvoker;
    private GraphQLObjectMapper objectMapper;
    private List<GraphQLServletListener> listeners;
    private boolean asyncServletModeEnabled;
    private Executor asyncExecutor;
    private long subscriptionTimeout;
    private ContextSetting contextSetting;

    public static GraphQLConfiguration.Builder with(GraphQLSchema schema) {
        return with(new DefaultGraphQLSchemaProvider(schema));
    }

    public static GraphQLConfiguration.Builder with(GraphQLSchemaProvider schemaProvider) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schemaProvider));
    }

    public static GraphQLConfiguration.Builder with(GraphQLInvocationInputFactory invocationInputFactory) {
        return new Builder(invocationInputFactory);
    }

    private GraphQLConfiguration(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker,
                                 GraphQLObjectMapper objectMapper, List<GraphQLServletListener> listeners, boolean asyncServletModeEnabled,
                                 Executor asyncExecutor, long subscriptionTimeout, ContextSetting contextSetting) {
        this.invocationInputFactory = invocationInputFactory;
        this.queryInvoker = queryInvoker;
        this.objectMapper = objectMapper;
        this.listeners = listeners;
        this.asyncServletModeEnabled = asyncServletModeEnabled;
        this.asyncExecutor = asyncExecutor;
        this.subscriptionTimeout = subscriptionTimeout;
        this.contextSetting = contextSetting;
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

    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    public void add(GraphQLServletListener listener) {
        listeners.add(listener);
    }

    public boolean remove(GraphQLServletListener listener) {
        return listeners.remove(listener);
    }

    public long getSubscriptionTimeout() {
        return subscriptionTimeout;
    }

    public ContextSetting getContextSetting() {
        return contextSetting;
    }

    public static class Builder {

        private GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder;
        private GraphQLInvocationInputFactory invocationInputFactory;
        private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
        private GraphQLObjectMapper objectMapper = GraphQLObjectMapper.newBuilder().build();
        private List<GraphQLServletListener> listeners = new ArrayList<>();
        private boolean asyncServletModeEnabled = false;
        private Executor asyncExecutor = Executors.newCachedThreadPool(new GraphQLThreadFactory());
        private long subscriptionTimeout = 0;
        private ContextSetting contextSetting = ContextSetting.PER_QUERY;

        private Builder(GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder) {
            this.invocationInputFactoryBuilder = invocationInputFactoryBuilder;
        }

        private Builder(GraphQLInvocationInputFactory invocationInputFactory) {
            this.invocationInputFactory = invocationInputFactory;
        }

        public Builder with(GraphQLQueryInvoker queryInvoker) {
            if (queryInvoker != null) {
                this.queryInvoker = queryInvoker;
            }
            return this;
        }

        public Builder with(GraphQLObjectMapper objectMapper) {
            if (objectMapper != null) {
                this.objectMapper = objectMapper;
            }
            return this;
        }

        public Builder with(List<GraphQLServletListener> listeners) {
            if (listeners != null) {
                this.listeners = listeners;
            }
            return this;
        }

        public Builder with(boolean asyncServletModeEnabled) {
            this.asyncServletModeEnabled = asyncServletModeEnabled;
            return this;
        }

        public Builder with(Executor asyncExecutor) {
            if (asyncExecutor != null) {
            	this.asyncExecutor = asyncExecutor;
            }
            return this;
        }

        public Builder with(GraphQLContextBuilder contextBuilder) {
            this.invocationInputFactoryBuilder.withGraphQLContextBuilder(contextBuilder);
            return this;
        }

        public Builder with(GraphQLRootObjectBuilder rootObjectBuilder) {
            this.invocationInputFactoryBuilder.withGraphQLRootObjectBuilder(rootObjectBuilder);
            return this;
        }

        public Builder with(long subscriptionTimeout) {
            this.subscriptionTimeout = subscriptionTimeout;
            return this;
        }

        public Builder with(ContextSetting contextSetting) {
            if (contextSetting != null) {
                this.contextSetting = contextSetting;
            }
            return this;
        }

        public GraphQLConfiguration build() {
            return new GraphQLConfiguration(
                    this.invocationInputFactory != null ? this.invocationInputFactory : invocationInputFactoryBuilder.build(),
                    queryInvoker,
                    objectMapper,
                    listeners,
                    asyncServletModeEnabled,
                    asyncExecutor,
                    subscriptionTimeout,
                    contextSetting
            );
        }

    }

}
