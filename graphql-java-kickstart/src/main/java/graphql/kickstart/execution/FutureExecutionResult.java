package graphql.kickstart.execution;

import graphql.ExecutionResult;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FutureExecutionResult {

  static FutureExecutionResult single(
      GraphQLInvocationInput invocationInput, CompletableFuture<ExecutionResult> single) {
    return new FutureSingleExecutionResult(invocationInput, single);
  }

  static FutureExecutionResult batched(
      GraphQLInvocationInput invocationInput, CompletableFuture<List<ExecutionResult>> batched) {
    return new FutureBatchedExecutionResult(invocationInput, batched);
  }

  static FutureExecutionResult error(GraphQLErrorQueryResult result) {
    return new FutureErrorExecutionResult(result);
  }

  CompletableFuture<GraphQLQueryResult> thenApplyQueryResult();

  GraphQLInvocationInput getInvocationInput();

  void cancel();
}
