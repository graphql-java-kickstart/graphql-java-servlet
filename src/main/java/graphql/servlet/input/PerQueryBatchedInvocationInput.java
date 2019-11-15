package graphql.servlet.input;

import graphql.schema.GraphQLSchema;
import graphql.servlet.context.ContextSetting;
import graphql.servlet.context.GraphQLContext;
import graphql.servlet.core.internal.GraphQLRequest;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * A Collection of GraphQLSingleInvocationInput that each have a unique context object.
 */
@Getter
public class PerQueryBatchedInvocationInput implements GraphQLBatchedInvocationInput {
    private final List<GraphQLSingleInvocationInput> executionInputs;
    private final ContextSetting contextSetting;

    public PerQueryBatchedInvocationInput(List<GraphQLRequest> requests, GraphQLSchema schema, Supplier<GraphQLContext> contextSupplier, Object root, ContextSetting contextSetting) {
        executionInputs = requests.stream()
            .map(request -> new GraphQLSingleInvocationInput(request, schema, contextSupplier.get(), root)).collect(Collectors.toList());
        this.contextSetting = contextSetting;
    }

}
