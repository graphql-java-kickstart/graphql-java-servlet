package graphql.kickstart.execution.input;

import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * A collection of GraphQLSingleInvocationInputs that share a context object.
 */
@Getter
public class PerRequestBatchedInvocationInput implements GraphQLBatchedInvocationInput {

  private final List<GraphQLSingleInvocationInput> invocationInputs;
  private final ContextSetting contextSetting;

  public PerRequestBatchedInvocationInput(List<GraphQLRequest> requests, GraphQLSchema schema,
      Supplier<GraphQLContext> contextSupplier, Object root, ContextSetting contextSetting) {
    GraphQLContext context = contextSupplier.get();
    invocationInputs = requests.stream().map(request -> new GraphQLSingleInvocationInput(request, schema, context, root))
        .collect(Collectors.toList());
    this.contextSetting = contextSetting;
  }

}
