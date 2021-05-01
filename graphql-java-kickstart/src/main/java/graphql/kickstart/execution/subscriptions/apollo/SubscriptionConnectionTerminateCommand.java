package graphql.kickstart.execution.subscriptions.apollo;

import static graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type.GQL_CONNECTION_TERMINATE;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class SubscriptionConnectionTerminateCommand implements SubscriptionCommand {

  private final Collection<ApolloSubscriptionConnectionListener> connectionListeners;

  @Override
  public void apply(SubscriptionSession session, OperationMessage message) {
    connectionListeners.forEach(it -> it.onTerminate(session, message));
    session.close("client requested " + GQL_CONNECTION_TERMINATE.getValue());
  }
}
