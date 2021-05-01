package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.util.Collection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SubscriptionStopCommand implements SubscriptionCommand {

  private final Collection<ApolloSubscriptionConnectionListener> connectionListeners;

  @Override
  public void apply(SubscriptionSession session, OperationMessage message) {
    connectionListeners.forEach(it -> it.onStop(session, message));
    session.unsubscribe(message.getId());
  }
}
