package graphql.servlet.input;

import graphql.schema.GraphQLSchema;
import graphql.servlet.context.GraphQLContext;
import graphql.servlet.core.internal.GraphQLRequest;

import java.util.List;
import java.util.stream.Collectors;

public class PerRequestBatchedInvocationInput implements GraphQLBatchedInvocationInput {

    private final List<GraphQLSingleInvocationInput> inputs;

    public PerRequestBatchedInvocationInput(List<GraphQLRequest> requests, GraphQLSchema schema, GraphQLContext context, Object root) {
        inputs = requests.stream().map(request -> new GraphQLSingleInvocationInput(request, schema, context, root)).collect(Collectors.toList());
    }

    @Override
    public List<GraphQLSingleInvocationInput> getExecutionInputs() {
        return inputs;
    }
}
