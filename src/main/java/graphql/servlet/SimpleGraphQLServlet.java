/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package graphql.servlet;

import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
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
        this(schema, executionStrategyProvider, null, null, null);
    }

    public SimpleGraphQLServlet(final GraphQLSchema schema, ExecutionStrategyProvider executionStrategyProvider, List<GraphQLServletListener> listeners, Instrumentation instrumentation, GraphQLErrorHandler errorHandler) {
        this(new DefaultGraphQLSchemaProvider(schema), executionStrategyProvider, listeners, instrumentation, errorHandler);
    }

    public SimpleGraphQLServlet(GraphQLSchemaProvider schemaProvider, ExecutionStrategyProvider executionStrategyProvider, List<GraphQLServletListener> listeners, Instrumentation instrumentation, GraphQLErrorHandler errorHandler) {
        super(listeners, null);

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
    }

    private final GraphQLSchemaProvider schemaProvider;
    private final ExecutionStrategyProvider executionStrategyProvider;
    private final Instrumentation instrumentation;
    private final GraphQLErrorHandler errorHandler;

    @Override
    protected GraphQLSchemaProvider getSchemaProvider() {
        return schemaProvider;
    }

    @Override
    protected GraphQLContext createContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response) {
        return new GraphQLContext(request, response);
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
    protected Map<String, Object> transformVariables(GraphQLSchema schema, String query, Map<String, Object> variables) {
        return variables;
    }

    @Override
    protected GraphQLErrorHandler getGraphQLErrorHandler() {
        return errorHandler;
    }
}
