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
import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Andrew Potter
 */
public class SimpleGraphQLServlet extends GraphQLServlet {

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy executionStrategy) {
        this(schema, executionStrategy, new ArrayList<>());
    }

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy executionStrategy, List<GraphQLOperationListener> operationListeners) {
        super(operationListeners);

        this.schema = schema;
        this.readOnlySchema = new GraphQLSchema(schema.getQueryType(), null, schema.getDictionary());

        this.executionStrategy = executionStrategy;
    }

    private final GraphQLSchema schema;
    private final GraphQLSchema readOnlySchema;
    private final ExecutionStrategy executionStrategy;

    @Override
    public GraphQLSchema getSchema() {
        return schema;
    }

    @Override
    public GraphQLSchema getReadOnlySchema() {
        return readOnlySchema;
    }

    @Override
    protected GraphQLContext createContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response) {
        return new GraphQLContext(request, response);
    }

    @Override
    protected ExecutionStrategy getExecutionStrategy() {
        return executionStrategy;
    }

    @Override
    protected Map<String, Object> transformVariables(GraphQLSchema schema, String query, Map<String, Object> variables) {
        return variables;
    }
}
