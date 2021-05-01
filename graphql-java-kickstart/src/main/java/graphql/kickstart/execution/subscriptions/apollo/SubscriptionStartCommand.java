package graphql.kickstart.execution.subscriptions.apollo;

import static graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type.GQL_ERROR;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class SubscriptionStartCommand implements SubscriptionCommand {

  private final GraphQLSubscriptionMapper mapper;
  private final GraphQLSubscriptionInvocationInputFactory invocationInputFactory;
  private final GraphQLInvoker graphQLInvoker;
  private final Collection<ApolloSubscriptionConnectionListener> connectionListeners;

  @Override
  public void apply(SubscriptionSession session, OperationMessage message) {
    log.debug("Apollo subscription start: {} --> {}", session, message.getPayload());
    connectionListeners.forEach(it -> it.onStart(session, message));
    CompletableFuture<ExecutionResult> executionResult =
        executeAsync(message.getPayload(), session);
    executionResult.thenAccept(result -> handleSubscriptionStart(session, message.getId(), result));
  }

  private CompletableFuture<ExecutionResult> executeAsync(
      Object payload, SubscriptionSession session) {
    Objects.requireNonNull(payload, "Payload is required");
    GraphQLRequest graphQLRequest = mapper.convertGraphQLRequest(payload);

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
      session.sendMessage(new OperationMessage(GQL_ERROR, id, payload));
    }
  }
}
