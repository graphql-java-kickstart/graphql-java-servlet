package graphql.kickstart.execution;

import static java.util.stream.Collectors.toList;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.kickstart.execution.config.GraphQLBuilder;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
public class GraphQLInvoker {

  private final GraphQLBuilder graphQLBuilder;
  private final BatchedDataLoaderGraphQLBuilder batchedDataLoaderGraphQLBuilder;
  private GraphQLInvokerProxy proxy = GraphQL::executeAsync;

  public CompletableFuture<ExecutionResult> executeAsync(GraphQLSingleInvocationInput invocationInput) {
    GraphQL graphQL = graphQLBuilder.build(invocationInput.getSchema());
    return proxy.executeAsync(graphQL, invocationInput.getExecutionInput());
  }

  public GraphQLQueryResult query(GraphQLInvocationInput invocationInput) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return GraphQLQueryResult.create(query((GraphQLSingleInvocationInput) invocationInput));
    }
    GraphQLBatchedInvocationInput batchedInvocationInput = (GraphQLBatchedInvocationInput) invocationInput;
    return GraphQLQueryResult.create(query(batchedInvocationInput));
  }

  private ExecutionResult query(GraphQLSingleInvocationInput singleInvocationInput) {
    return executeAsync(singleInvocationInput).join();
  }

  private List<ExecutionResult> query(GraphQLBatchedInvocationInput batchedInvocationInput) {
    GraphQL graphQL = batchedDataLoaderGraphQLBuilder.newGraphQL(batchedInvocationInput, graphQLBuilder);
    return batchedInvocationInput.getExecutionInputs().stream()
        .map(executionInput -> proxy.executeAsync(graphQL, executionInput))
        .collect(toList())
        .stream()
        .map(CompletableFuture::join)
        .collect(toList());
  }
}

