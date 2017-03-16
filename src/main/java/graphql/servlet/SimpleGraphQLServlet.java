package graphql.servlet;

import graphql.execution.ExecutionStrategy;
import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * @author Andrew Potter
 */
public class SimpleGraphQLServlet extends GraphQLServlet {

    public SimpleGraphQLServlet(GraphQLSchema schema, ExecutionStrategy executionStrategy) {
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
