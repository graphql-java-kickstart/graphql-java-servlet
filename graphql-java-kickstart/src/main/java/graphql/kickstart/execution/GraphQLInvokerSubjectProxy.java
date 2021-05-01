package graphql.kickstart.execution;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.kickstart.execution.context.GraphQLContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import javax.security.auth.Subject;

public class GraphQLInvokerSubjectProxy implements GraphQLInvokerProxy {

  @Override
  public CompletableFuture<ExecutionResult> executeAsync(
      GraphQL graphQL, ExecutionInput executionInput) {
    GraphQLContext context = (GraphQLContext) executionInput.getContext();
    if (Subject.getSubject(AccessController.getContext()) == null
        && context.getSubject().isPresent()) {
      return context
          .getSubject()
          .map(it -> Subject.doAs(it, doAction(graphQL, executionInput)))
          .orElseGet(() -> graphQL.executeAsync(executionInput));
    }
    return graphQL.executeAsync(executionInput);
  }

  private PrivilegedAction<CompletableFuture<ExecutionResult>> doAction(
      GraphQL graphQL, ExecutionInput executionInput) {
    return () -> graphQL.executeAsync(executionInput);
  }
}
