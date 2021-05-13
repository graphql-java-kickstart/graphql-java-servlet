package graphql.kickstart.execution;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FutureErrorExecutionResult implements FutureExecutionResult {

  private final GraphQLErrorQueryResult errorQueryResult;

  @Override
  public CompletableFuture<GraphQLQueryResult> thenApplyQueryResult() {
    return CompletableFuture.completedFuture(errorQueryResult);
  }

  @Override
  public GraphQLInvocationInput getInvocationInput() {
    return null;
  }

  @Override
  public void cancel() {
    // nothing to do
  }
}
