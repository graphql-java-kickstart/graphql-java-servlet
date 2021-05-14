package graphql.kickstart.execution.input;

import static java.util.stream.Collectors.toList;

import graphql.ExecutionInput;
import graphql.kickstart.execution.context.ContextSetting;
import java.util.List;

/** Interface representing a batched input. */
public interface GraphQLBatchedInvocationInput extends GraphQLInvocationInput {

  /** @return each individual input in the batch, configured with a context. */
  List<GraphQLSingleInvocationInput> getInvocationInputs();

  default List<ExecutionInput> getExecutionInputs() {
    return getInvocationInputs().stream()
        .map(GraphQLSingleInvocationInput::getExecutionInput)
        .collect(toList());
  }

  ContextSetting getContextSetting();

}
