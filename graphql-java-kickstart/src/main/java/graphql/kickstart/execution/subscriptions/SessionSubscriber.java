package graphql.kickstart.execution.subscriptions;

import static java.util.Collections.singletonList;

import graphql.ExecutionResult;
import graphql.GraphqlErrorBuilder;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.kickstart.execution.error.GenericGraphQLError;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
@RequiredArgsConstructor
class SessionSubscriber implements Subscriber<ExecutionResult> {

  private final SubscriptionSession session;
  private final String id;
  private AtomicSubscriptionSubscription subscriptionReference =
      new AtomicSubscriptionSubscription();

  @Override
  public void onSubscribe(Subscription subscription) {
    log.debug("Subscribe to execution result: {}", subscription);
    subscriptionReference.set(subscription);
    subscriptionReference.get().request(1);

    session.add(id, subscriptionReference.get());
  }

  @Override
  public void onNext(ExecutionResult executionResult) {
    Map<String, Object> result = new HashMap<>();
    result.put("data", executionResult.getData());

    session.sendDataMessage(id, result);
    subscriptionReference.get().request(1);
  }

  @Override
  public void onError(Throwable throwable) {
    log.error("Subscription error", throwable);
    Map<String, Object> payload = new HashMap<>();
    if (throwable.getCause() instanceof NonNullableFieldWasNullException) {
      NonNullableFieldWasNullException e = (NonNullableFieldWasNullException) throwable.getCause();
      payload.put(
          "errors",
          singletonList(
              GraphqlErrorBuilder.newError().message(e.getMessage()).path(e.getPath()).build()));
    } else {
      payload.put("errors", singletonList(new GenericGraphQLError(throwable.getMessage())));
    }

    session.unsubscribe(id);
    session.sendErrorMessage(id, payload);
  }

  @Override
  public void onComplete() {
    session.unsubscribe(id);
    session.sendCompleteMessage(id);
  }
}
