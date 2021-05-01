package graphql.kickstart.execution;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.concurrent.CompletableFuture;

public interface GraphQLInvokerProxy {

  CompletableFuture<ExecutionResult> executeAsync(GraphQL graphQL, ExecutionInput executionInput);
}
