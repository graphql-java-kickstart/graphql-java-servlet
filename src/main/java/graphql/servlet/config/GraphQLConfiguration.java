package graphql.servlet.config;

import graphql.schema.GraphQLSchema;
import graphql.servlet.context.GraphQLContextBuilder;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.core.GraphQLQueryInvoker;
import graphql.servlet.core.GraphQLRootObjectBuilder;
import graphql.servlet.core.GraphQLServletListener;
import graphql.servlet.context.ContextSetting;
import graphql.servlet.input.BatchInputPreProcessor;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import graphql.servlet.core.internal.GraphQLThreadFactory;
import graphql.servlet.input.NoOpBatchInputPreProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class GraphQLConfiguration {

    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final Supplier<BatchInputPreProcessor> batchInputPreProcessor;
    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLObjectMapper objectMapper;
    private final List<GraphQLServletListener> listeners;
    private final boolean asyncServletModeEnabled;
    private final Executor asyncExecutor;
    private final long subscriptionTimeout;
    private final ContextSetting contextSetting;

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
                                 Executor asyncExecutor, long subscriptionTimeout, ContextSetting contextSetting,
                                 Supplier<BatchInputPreProcessor> batchInputPreProcessor) {
        this.invocationInputFactory = invocationInputFactory;
        this.queryInvoker = queryInvoker;
        this.objectMapper = objectMapper;
        this.listeners = listeners;
        this.asyncServletModeEnabled = asyncServletModeEnabled;
        this.asyncExecutor = asyncExecutor;
        this.subscriptionTimeout = subscriptionTimeout;
        this.contextSetting = contextSetting;
        this.batchInputPreProcessor = batchInputPreProcessor;
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

    public BatchInputPreProcessor getBatchInputPreProcessor() {
        return batchInputPreProcessor.get();
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
        private Supplier<BatchInputPreProcessor> batchInputPreProcessorSupplier = () -> new NoOpBatchInputPreProcessor();

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

        public Builder with(BatchInputPreProcessor batchInputPreProcessor) {
            if (batchInputPreProcessor != null) {
                this.batchInputPreProcessorSupplier = () -> batchInputPreProcessor;
            }
            return this;
        }

        public Builder with(Supplier<BatchInputPreProcessor> batchInputPreProcessor) {
            if (batchInputPreProcessor != null) {
                this.batchInputPreProcessorSupplier = batchInputPreProcessor;
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
                    contextSetting,
                    batchInputPreProcessorSupplier
            );
        }

    }

}
