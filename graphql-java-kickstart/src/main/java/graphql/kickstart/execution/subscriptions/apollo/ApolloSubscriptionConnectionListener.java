package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.SubscriptionConnectionListener;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;

public interface ApolloSubscriptionConnectionListener extends SubscriptionConnectionListener {

  default void onConnect(SubscriptionSession session, OperationMessage message) {
    // do nothing
  }

  default void onStart(SubscriptionSession session, OperationMessage message) {
    // do nothing
  }

  default void onStop(SubscriptionSession session, OperationMessage message) {
    // do nothing
  }

  default void onTerminate(SubscriptionSession session, OperationMessage message) {
    // do nothing
  }

  default void shutdown() {
    // do nothing
  }
}