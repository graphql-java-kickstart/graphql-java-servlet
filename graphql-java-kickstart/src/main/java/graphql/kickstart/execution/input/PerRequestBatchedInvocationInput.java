package graphql.kickstart.execution.input;

import static java.util.stream.Collectors.toList;

import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;

/** A collection of GraphQLSingleInvocationInputs that share a context object. */
@Getter
public class PerRequestBatchedInvocationInput implements GraphQLBatchedInvocationInput {

  private final List<GraphQLSingleInvocationInput> invocationInputs;
  private final ContextSetting contextSetting;

  public PerRequestBatchedInvocationInput(
      List<GraphQLRequest> requests,
      GraphQLSchema schema,
      Supplier<GraphQLKickstartContext> contextSupplier,
      Object root,
      ContextSetting contextSetting) {
    GraphQLKickstartContext context = contextSupplier.get();
    invocationInputs =
        requests.stream()
            .map(request -> new GraphQLSingleInvocationInput(request, schema, context, root))
            .collect(toList());
    this.contextSetting = contextSetting;
  }

  @Override
  public List<String> getQueries() {
    return invocationInputs.stream()
        .map(GraphQLSingleInvocationInput::getQueries)
        .flatMap(List::stream)
        .collect(toList());
  }
}
