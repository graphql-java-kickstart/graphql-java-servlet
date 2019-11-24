package graphql.kickstart.execution;

import static java.util.stream.Collectors.toList;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
public class GraphQLInvoker {

  private final GraphQL graphQL;
  private GraphQLInvokerProxy proxy = GraphQL::executeAsync;

  public CompletableFuture<ExecutionResult> executeAsync(GraphQLSingleInvocationInput invocationInput) {
    return proxy.executeAsync(graphQL, invocationInput.getExecutionInput());
  }

  public GraphQLQueryResult query(GraphQLInvocationInput invocationInput) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return GraphQLQueryResult.create(query((GraphQLSingleInvocationInput) invocationInput));
    }
    GraphQLBatchedInvocationInput batchedInvocationInput = (GraphQLBatchedInvocationInput) invocationInput;
    return GraphQLQueryResult
        .create(query(batchedInvocationInput.getExecutionInputs(), batchedInvocationInput.getContextSetting()));
  }

  private ExecutionResult query(GraphQLSingleInvocationInput singleInvocationInput) {
    return executeAsync(singleInvocationInput).join();
  }

  private List<ExecutionResult> query(List<GraphQLSingleInvocationInput> batchedInvocationInput,
      ContextSetting contextSetting) {
//    List<ExecutionInput> executionIds = batchedInvocationInput.stream()
//        .map(GraphQLSingleInvocationInput::getExecutionInput)
//        .collect(Collectors.toList());
//    Supplier<Instrumentation> configuredInstrumentation = contextSetting
//        .configureInstrumentationForContext(getInstrumentation, executionIds, optionsSupplier.get());
    return batchedInvocationInput.stream()
        .map(this::executeAsync)
        .map(CompletableFuture::join)
        .collect(toList());
  }
}
