package graphql.kickstart.execution.input;

import static java.util.stream.Collectors.toList;

import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;

/** A Collection of GraphQLSingleInvocationInput that each have a unique context object. */
@Getter
public class PerQueryBatchedInvocationInput implements GraphQLBatchedInvocationInput {

  private final List<GraphQLSingleInvocationInput> invocationInputs;
  private final ContextSetting contextSetting;

  public PerQueryBatchedInvocationInput(
      List<GraphQLRequest> requests,
      GraphQLSchema schema,
      Supplier<GraphQLKickstartContext> contextSupplier,
      Object root,
      ContextSetting contextSetting) {
    invocationInputs =
        requests.stream()
            .map(
                request ->
                    new GraphQLSingleInvocationInput(request, schema, contextSupplier.get(), root))
            .collect(Collectors.toList());
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
