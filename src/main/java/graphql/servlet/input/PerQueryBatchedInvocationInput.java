package graphql.servlet.input;

import graphql.ExecutionInput;
import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLContext;
import graphql.servlet.internal.GraphQLRequest;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Andrew Potter
 */
public class PerQueryBatchedInvocationInput implements GraphQLBatchedInvocationInput {
    private final List<GraphQLSingleInvocationInput> inputs;

    public PerQueryBatchedInvocationInput(List<GraphQLRequest> requests, GraphQLSchema schema, Supplier<GraphQLContext> contextSupplier, Object root) {
        inputs = requests.stream()
            .map(request -> new GraphQLSingleInvocationInput(request, schema, contextSupplier.get(), root)).collect(Collectors.toList());
    }

    public List<GraphQLSingleInvocationInput> getExecutionInputs() {
        return inputs;
    }
}
