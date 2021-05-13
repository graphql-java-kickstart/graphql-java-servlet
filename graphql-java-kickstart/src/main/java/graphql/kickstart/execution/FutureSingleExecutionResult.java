package graphql.kickstart.execution;

import graphql.ExecutionResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FutureSingleExecutionResult implements FutureExecutionResult {

  @Getter
  private final GraphQLInvocationInput invocationInput;
  private final CompletableFuture<ExecutionResult> single;

  @Override
  public CompletableFuture<GraphQLQueryResult> thenApplyQueryResult() {
    return single.thenApply(GraphQLQueryResult::create);
  }

  @Override
  public void cancel() {
    single.cancel(true);
  }
}
