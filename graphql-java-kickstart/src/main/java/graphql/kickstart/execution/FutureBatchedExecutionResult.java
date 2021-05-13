package graphql.kickstart.execution;

import graphql.ExecutionResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FutureBatchedExecutionResult implements FutureExecutionResult {

  @Getter
  private final GraphQLInvocationInput invocationInput;
  private final CompletableFuture<List<ExecutionResult>> batched;

  @Override
  public CompletableFuture<GraphQLQueryResult> thenApplyQueryResult() {
    return batched.thenApply(GraphQLQueryResult::create);
  }

  @Override
  public void cancel() {
    batched.cancel(true);
  }
}
