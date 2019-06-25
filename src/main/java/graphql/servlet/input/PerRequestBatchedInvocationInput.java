package graphql.servlet.input;

import graphql.schema.GraphQLSchema;
import graphql.servlet.context.GraphQLContext;
import graphql.servlet.core.internal.GraphQLRequest;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A collection of GraphQLSingleInvocationInputs that share a context object.
 */
public class PerRequestBatchedInvocationInput implements GraphQLBatchedInvocationInput {

    private final List<GraphQLSingleInvocationInput> inputs;

    public PerRequestBatchedInvocationInput(List<GraphQLRequest> requests, GraphQLSchema schema, Supplier<GraphQLContext> contextSupplier, Object root) {
        GraphQLContext context = contextSupplier.get();
        inputs = requests.stream().map(request -> new GraphQLSingleInvocationInput(request, schema, context, root)).collect(Collectors.toList());
    }

    @Override
    public List<GraphQLSingleInvocationInput> getExecutionInputs() {
        return inputs;
    }
}
