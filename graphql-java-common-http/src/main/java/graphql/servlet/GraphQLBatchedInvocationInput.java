package graphql.servlet;

import graphql.ExecutionInput;
import graphql.schema.GraphQLSchema;
import graphql.servlet.internal.GraphQLRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Potter
 */
public class GraphQLBatchedInvocationInput extends GraphQLInvocationInput {
    private final List<GraphQLRequest> requests;

    public GraphQLBatchedInvocationInput(List<GraphQLRequest> requests, GraphQLSchema schema, GraphQLContext context, Object root) {
        super(schema, context, root);
        this.requests = Collections.unmodifiableList(requests);
    }

    public List<ExecutionInput> getExecutionInputs() {
        return requests.stream()
                .map(this::createExecutionInput)
                .collect(Collectors.toList());
    }

}
