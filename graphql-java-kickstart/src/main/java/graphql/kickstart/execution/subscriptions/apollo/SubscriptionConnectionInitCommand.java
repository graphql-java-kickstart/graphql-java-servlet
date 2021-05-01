package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class SubscriptionConnectionInitCommand implements SubscriptionCommand {

  private final Collection<ApolloSubscriptionConnectionListener> connectionListeners;

  @Override
  public void apply(SubscriptionSession session, OperationMessage message) {
    log.debug("Apollo subscription connection init: {}", session);
    try {
      connectionListeners.forEach(it -> it.onConnect(session, message));
      session.sendMessage(new OperationMessage(Type.GQL_CONNECTION_ACK, message.getId(), null));
    } catch (Exception t) {
      log.error("Cannot initialize subscription command '{}'", message, t);
      session.sendMessage(
          new OperationMessage(Type.GQL_CONNECTION_ERROR, message.getId(), t.getMessage()));
    }
  }
}
