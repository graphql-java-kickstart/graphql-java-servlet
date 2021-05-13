package graphql.kickstart.execution;

import static java.util.stream.Collectors.toList;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.kickstart.execution.config.GraphQLBuilder;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GraphQLInvoker {

  private final GraphQLBuilder graphQLBuilder;
  private final BatchedDataLoaderGraphQLBuilder batchedDataLoaderGraphQLBuilder;
  private final GraphQLInvokerProxy proxy = GraphQL::executeAsync;

  public FutureExecutionResult execute(GraphQLInvocationInput invocationInput) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return FutureExecutionResult.single(
          invocationInput, executeAsync((GraphQLSingleInvocationInput) invocationInput));
    }
    return FutureExecutionResult.batched(
        invocationInput, executeAsync((GraphQLBatchedInvocationInput) invocationInput));
  }

  public CompletableFuture<ExecutionResult> executeAsync(
      GraphQLSingleInvocationInput invocationInput) {
    GraphQL graphQL = graphQLBuilder.build(invocationInput.getSchema());
    return proxy.executeAsync(graphQL, invocationInput.getExecutionInput());
  }

  public GraphQLQueryResult query(GraphQLInvocationInput invocationInput) {
    return queryAsync(invocationInput).join();
  }

  public CompletableFuture<GraphQLQueryResult> queryAsync(GraphQLInvocationInput invocationInput) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return executeAsync((GraphQLSingleInvocationInput) invocationInput)
          .thenApply(GraphQLQueryResult::create);
    }
    GraphQLBatchedInvocationInput batchedInvocationInput =
        (GraphQLBatchedInvocationInput) invocationInput;
    return executeAsync(batchedInvocationInput).thenApply(GraphQLQueryResult::create);
  }

  private CompletableFuture<List<ExecutionResult>> executeAsync(
      GraphQLBatchedInvocationInput batchedInvocationInput) {
    GraphQL graphQL =
        batchedDataLoaderGraphQLBuilder.newGraphQL(batchedInvocationInput, graphQLBuilder);
    return sequence(
        batchedInvocationInput.getExecutionInputs().stream()
            .map(executionInput -> proxy.executeAsync(graphQL, executionInput))
            .collect(toList()));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
    CompletableFuture[] futuresArray = futures.toArray(new CompletableFuture[0]);
    return CompletableFuture.allOf(futuresArray)
        .thenApply(
            aVoid -> {
              List<T> result = new ArrayList<>(futures.size());
              for (CompletableFuture future : futuresArray) {
                assert future.isDone(); // per the API contract of allOf()
                result.add((T) future.join());
              }
              return result;
            });
  }
}
