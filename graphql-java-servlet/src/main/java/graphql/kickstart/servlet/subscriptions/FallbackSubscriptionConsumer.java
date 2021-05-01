package graphql.kickstart.servlet.subscriptions;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

/** @author Andrew Potter */
@RequiredArgsConstructor
public class FallbackSubscriptionConsumer implements Consumer<String> {

  private final SubscriptionSession session;
  private final GraphQLSubscriptionMapper mapper;
  private final GraphQLSubscriptionInvocationInputFactory invocationInputFactory;
  private final GraphQLInvoker graphQLInvoker;

  @Override
  public void accept(String text) {
    CompletableFuture<ExecutionResult> executionResult = executeAsync(text, session);
    executionResult.thenAccept(
        result -> handleSubscriptionStart(session, UUID.randomUUID().toString(), result));
  }

  private CompletableFuture<ExecutionResult> executeAsync(
      String payload, SubscriptionSession session) {
    Objects.requireNonNull(payload, "Payload is required");
    GraphQLRequest graphQLRequest = mapper.readGraphQLRequest(payload);

    GraphQLSingleInvocationInput invocationInput =
        invocationInputFactory.create(graphQLRequest, session);
    return graphQLInvoker.executeAsync(invocationInput);
  }

  private void handleSubscriptionStart(
      SubscriptionSession session, String id, ExecutionResult executionResult) {
    ExecutionResult sanitizedExecutionResult = mapper.sanitizeErrors(executionResult);
    if (mapper.hasNoErrors(sanitizedExecutionResult)) {
      session.subscribe(id, sanitizedExecutionResult.getData());
    } else {
      Object payload = mapper.convertSanitizedExecutionResult(sanitizedExecutionResult);
      session.sendDataMessage(id, payload);
    }
  }
}
