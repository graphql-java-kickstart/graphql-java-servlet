package graphql.servlet;

import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Potter
 */
public class SimpleGraphQLServlet extends GraphQLServlet {

    /**
     * @deprecated use {@link #builder(GraphQLSchema)} instead.
     */
    @Deprecated
    public SimpleGraphQLServlet(GraphQLSchema schema) {
        this(schema, new DefaultExecutionStrategyProvider());
    }

    /**
     * @deprecated use {@link #builder(GraphQLSchema)} instead.
     */
    @Deprecated
    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy executionStrategy) {
        this(schema, new DefaultExecutionStrategyProvider(executionStrategy));
    }

    /**
     * @deprecated use {@link #builder(GraphQLSchema)} instead.
     */
    @Deprecated
    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategyProvider executionStrategyProvider) {
        this(schema, executionStrategyProvider, null, null, null, null, null, null, null);
    }

    /**
     * @deprecated use {@link #builder(GraphQLSchema)} instead.
     */
    @Deprecated
    public SimpleGraphQLServlet(final GraphQLSchema schema, ExecutionStrategyProvider executionStrategyProvider, ObjectMapperConfigurer objectMapperConfigurer, List<GraphQLServletListener> listeners, Instrumentation instrumentation, GraphQLErrorHandler errorHandler, GraphQLContextBuilder contextBuilder, GraphQLRootObjectBuilder rootObjectBuilder, PreparsedDocumentProvider preparsedDocumentProvider) {
        this(new DefaultGraphQLSchemaProvider(schema), executionStrategyProvider, objectMapperConfigurer, listeners, instrumentation, errorHandler, contextBuilder, rootObjectBuilder, preparsedDocumentProvider);
    }

    /**
     * @deprecated use {@link #builder(GraphQLSchemaProvider)} instead.
     */
    @Deprecated
    public SimpleGraphQLServlet(GraphQLSchemaProvider schemaProvider, ExecutionStrategyProvider executionStrategyProvider, ObjectMapperConfigurer objectMapperConfigurer, List<GraphQLServletListener> listeners, Instrumentation instrumentation, GraphQLErrorHandler errorHandler, GraphQLContextBuilder contextBuilder, GraphQLRootObjectBuilder rootObjectBuilder, PreparsedDocumentProvider preparsedDocumentProvider) {
        super(objectMapperConfigurer, listeners);

        this.schemaProvider = schemaProvider;
        this.executionStrategyProvider = executionStrategyProvider;

        if (instrumentation == null) {
            this.instrumentation = SimpleInstrumentation.INSTANCE;
        } else {
            this.instrumentation = instrumentation;
        }

        if(errorHandler == null) {
            this.errorHandler = new DefaultGraphQLErrorHandler();
        } else {
            this.errorHandler = errorHandler;
        }

        if(contextBuilder == null) {
            this.contextBuilder = new DefaultGraphQLContextBuilder();
        } else {
            this.contextBuilder = contextBuilder;
        }

        if(rootObjectBuilder == null) {
            this.rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
        } else {
            this.rootObjectBuilder = rootObjectBuilder;
        }

        if(preparsedDocumentProvider == null) {
            this.preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;
        } else {
            this.preparsedDocumentProvider = preparsedDocumentProvider;
        }
    }

    protected SimpleGraphQLServlet(Builder builder) {
        super(builder.objectMapperConfigurer, builder.listeners);

        this.schemaProvider = builder.schemaProvider;
        this.executionStrategyProvider = builder.executionStrategyProvider;
        this.instrumentation = builder.instrumentation;
        this.errorHandler = builder.errorHandler;
        this.contextBuilder = builder.contextBuilder;
        this.rootObjectBuilder = builder.rootObjectBuilder;
        this.preparsedDocumentProvider = builder.preparsedDocumentProvider;
    }

    private final GraphQLSchemaProvider schemaProvider;
    private final ExecutionStrategyProvider executionStrategyProvider;
    private final Instrumentation instrumentation;
    private final GraphQLErrorHandler errorHandler;
    private final GraphQLContextBuilder contextBuilder;
    private final GraphQLRootObjectBuilder rootObjectBuilder;
    private final PreparsedDocumentProvider preparsedDocumentProvider;

    public static SimpleGraphQLServlet create(GraphQLSchema schema) {
        return new Builder(schema).build();
    }

    public static SimpleGraphQLServlet create(GraphQLSchemaProvider schemaProvider) {
        return new Builder(schemaProvider).build();
    }

    public static Builder builder(GraphQLSchema schema) {
        return new Builder(schema);
    }

    public static Builder builder(GraphQLSchemaProvider schemaProvider) {
        return new Builder(schemaProvider);
    }

    public static class Builder {
        private final GraphQLSchemaProvider schemaProvider;
        private ExecutionStrategyProvider executionStrategyProvider = new DefaultExecutionStrategyProvider();
        private ObjectMapperConfigurer objectMapperConfigurer;
        private List<GraphQLServletListener> listeners;
        private Instrumentation instrumentation = SimpleInstrumentation.INSTANCE;
        private GraphQLErrorHandler errorHandler = new DefaultGraphQLErrorHandler();
        private GraphQLContextBuilder contextBuilder = new DefaultGraphQLContextBuilder();
        private GraphQLRootObjectBuilder rootObjectBuilder = new DefaultGraphQLRootObjectBuilder();
        private PreparsedDocumentProvider preparsedDocumentProvider = NoOpPreparsedDocumentProvider.INSTANCE;

        public Builder(GraphQLSchema schema) {
            this(new DefaultGraphQLSchemaProvider(schema));
        }

        public Builder(GraphQLSchemaProvider schemaProvider) {
            this.schemaProvider = schemaProvider;
        }

        public Builder withExecutionStrategyProvider(ExecutionStrategyProvider provider) {
            this.executionStrategyProvider = provider;
            return this;
        }

        public Builder withObjectMapperConfigurer(ObjectMapperConfigurer configurer) {
            this.objectMapperConfigurer = configurer;
            return this;
        }

        public Builder withInstrumentation(Instrumentation instrumentation) {
            this.instrumentation = instrumentation;
            return this;
        }

        public Builder withGraphQLErrorHandler(GraphQLErrorHandler handler) {
            this.errorHandler = handler;
            return this;
        }

        public Builder withGraphQLContextBuilder(GraphQLContextBuilder context) {
            this.contextBuilder = context;
            return this;
        }

        public Builder withGraphQLRootObjectBuilder(GraphQLRootObjectBuilder rootObject) {
            this.rootObjectBuilder = rootObject;
            return this;
        }

        public Builder withPreparsedDocumentProvider(PreparsedDocumentProvider provider) {
            this.preparsedDocumentProvider = provider;
            return this;
        }

        public Builder withListeners(List<GraphQLServletListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public SimpleGraphQLServlet build() {
            return new SimpleGraphQLServlet(this);
        }
    }

    @Override
    protected GraphQLSchemaProvider getSchemaProvider() {
        return schemaProvider;
    }

    @Override
    protected GraphQLContext createContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response) {
        return this.contextBuilder.build(request, response);
    }

    @Override
    protected Object createRootObject(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response) {
        return this.rootObjectBuilder.build(request, response);
    }

    @Override
    protected ExecutionStrategyProvider getExecutionStrategyProvider() {
        return executionStrategyProvider;
    }

    @Override
    protected Instrumentation getInstrumentation() {
        return instrumentation;
    }

    @Override
    protected GraphQLErrorHandler getGraphQLErrorHandler() {
        return errorHandler;
    }

    @Override
    protected PreparsedDocumentProvider getPreparsedDocumentProvider() {
        return preparsedDocumentProvider;
    }
}
