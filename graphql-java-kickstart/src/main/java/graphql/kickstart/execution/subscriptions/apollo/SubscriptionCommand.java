package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;

interface SubscriptionCommand {

  void apply(SubscriptionSession session, OperationMessage message);
}
