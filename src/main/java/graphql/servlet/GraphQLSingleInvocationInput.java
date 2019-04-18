package graphql.servlet;

import graphql.ExecutionInput;
import graphql.schema.GraphQLSchema;
import graphql.servlet.internal.GraphQLRequest;

/**
 * @author Andrew Potter
 */
public class GraphQLSingleInvocationInput extends GraphQLInvocationInput {

    private final GraphQLRequest request;

    public GraphQLSingleInvocationInput(GraphQLRequest request, GraphQLSchema schema, GraphQLContext context, Object root) {
        super(schema, context, root);

        this.request = request;
    }

    public ExecutionInput getExecutionInput() {
        return createExecutionInput(request);
    }
}
