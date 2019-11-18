package graphql.kickstart.execution;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GraphQLInvoker {

  private final GraphQL graphQL;

  public GraphQLQueryResult query(GraphQLInvocationInput invocationInput) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return GraphQLQueryResult.create(query((GraphQLSingleInvocationInput) invocationInput));
    }
    GraphQLBatchedInvocationInput batchedInvocationInput = (GraphQLBatchedInvocationInput) invocationInput;
    return GraphQLQueryResult.create(query(batchedInvocationInput.getExecutionInputs(), batchedInvocationInput.getContextSetting()));
  }

  private ExecutionResult query(GraphQLSingleInvocationInput singleInvocationInput) {
    return executeAsync(singleInvocationInput).join();
  }

  public CompletableFuture<ExecutionResult> executeAsync(GraphQLSingleInvocationInput invocationInput) {
    if (Subject.getSubject(AccessController.getContext()) == null && invocationInput.getSubject().isPresent()) {
      return Subject
          .doAs(invocationInput.getSubject().get(), (PrivilegedAction<CompletableFuture<ExecutionResult>>) () -> {
            try {
              return query(invocationInput.getExecutionInput());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }

    return query(invocationInput.getExecutionInput());
  }

  private CompletableFuture<ExecutionResult> query(ExecutionInput executionInput) {
    return graphQL.executeAsync(executionInput);
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
        //We want eager eval
        .collect(Collectors.toList())
        .stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
  }
}
