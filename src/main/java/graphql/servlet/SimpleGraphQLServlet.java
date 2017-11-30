package graphql.servlet;

import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.NoOpInstrumentation;
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

    public SimpleGraphQLServlet(GraphQLSchema schema) {
        this(schema, new DefaultExecutionStrategyProvider());
    }

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy executionStrategy) {
        this(schema, new DefaultExecutionStrategyProvider(executionStrategy));
    }

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategyProvider executionStrategyProvider) {
        this(schema, executionStrategyProvider, null, null, null, null, null, null, null);
    }

    public SimpleGraphQLServlet(final GraphQLSchema schema, ExecutionStrategyProvider executionStrategyProvider, ObjectMapperConfigurer objectMapperConfigurer, List<GraphQLServletListener> listeners, Instrumentation instrumentation, GraphQLErrorHandler errorHandler, GraphQLContextBuilder contextBuilder, GraphQLRootObjectBuilder rootObjectBuilder, PreparsedDocumentProvider preparsedDocumentProvider) {
        this(new DefaultGraphQLSchemaProvider(schema), executionStrategyProvider, objectMapperConfigurer, listeners, instrumentation, errorHandler, contextBuilder, rootObjectBuilder, preparsedDocumentProvider);
    }

    public SimpleGraphQLServlet(GraphQLSchemaProvider schemaProvider, ExecutionStrategyProvider executionStrategyProvider, ObjectMapperConfigurer objectMapperConfigurer, List<GraphQLServletListener> listeners, Instrumentation instrumentation, GraphQLErrorHandler errorHandler, GraphQLContextBuilder contextBuilder, GraphQLRootObjectBuilder rootObjectBuilder, PreparsedDocumentProvider preparsedDocumentProvider) {
        super(objectMapperConfigurer, listeners, null);

        this.schemaProvider = schemaProvider;
        this.executionStrategyProvider = executionStrategyProvider;

        if (instrumentation == null) {
            this.instrumentation = NoOpInstrumentation.INSTANCE;
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

    private final GraphQLSchemaProvider schemaProvider;
    private final ExecutionStrategyProvider executionStrategyProvider;
    private final Instrumentation instrumentation;
    private final GraphQLErrorHandler errorHandler;
    private final GraphQLContextBuilder contextBuilder;
    private final GraphQLRootObjectBuilder rootObjectBuilder;
    private final PreparsedDocumentProvider preparsedDocumentProvider;

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
