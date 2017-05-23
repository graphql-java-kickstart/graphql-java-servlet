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
import graphql.execution.SimpleExecutionStrategy;
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
        this(schema, new SimpleExecutionStrategy());
    }

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy queryExecutionStrategy) {
        this(schema, queryExecutionStrategy, queryExecutionStrategy);
    }

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy queryExecutionStrategy, ExecutionStrategy mutationExecutionStrategy) {
        this(schema, queryExecutionStrategy, mutationExecutionStrategy, null, null);
    }

    public SimpleGraphQLServlet(final GraphQLSchema schema, ExecutionStrategy queryExecutionStrategy, ExecutionStrategy mutationExecutionStrategy, List<GraphQLServletListener> listeners, Instrumentation instrumentation) {
        this(new DefaultGraphQLSchemaProvider(schema), queryExecutionStrategy, mutationExecutionStrategy, listeners, instrumentation);
    }

    public SimpleGraphQLServlet(GraphQLSchemaProvider schemaProvider, ExecutionStrategy queryExecutionStrategy, ExecutionStrategy mutationExecutionStrategy, List<GraphQLServletListener> listeners, Instrumentation instrumentation) {
        super(listeners, null);

        this.schemaProvider = schemaProvider;
        this.queryExecutionStrategy = queryExecutionStrategy;
        this.mutationExecutionStrategy = mutationExecutionStrategy;

        if (instrumentation == null) {
            this.instrumentation = NoOpInstrumentation.INSTANCE;
        } else {
            this.instrumentation = instrumentation;
        }
    }

    private final GraphQLSchemaProvider schemaProvider;
    private final ExecutionStrategy queryExecutionStrategy;
    private final ExecutionStrategy mutationExecutionStrategy;
    private final Instrumentation instrumentation;

    @Override
    protected GraphQLSchemaProvider getSchemaProvider() {
        return schemaProvider;
    }

    @Override
    protected GraphQLContext createContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response) {
        return new GraphQLContext(request, response);
    }

    @Override
    protected ExecutionStrategy getQueryExecutionStrategy() {
        return queryExecutionStrategy;
    }

    @Override
    protected ExecutionStrategy getMutationExecutionStrategy() {
        return mutationExecutionStrategy;
    }

    @Override
    protected Instrumentation getInstrumentation() {
        return instrumentation;
    }

    @Override
    protected Map<String, Object> transformVariables(GraphQLSchema schema, String query, Map<String, Object> variables) {
        return variables;
    }
}
