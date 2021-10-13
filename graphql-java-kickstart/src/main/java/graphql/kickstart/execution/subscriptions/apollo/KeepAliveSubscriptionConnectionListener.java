package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.time.Duration;

public class KeepAliveSubscriptionConnectionListener
    implements ApolloSubscriptionConnectionListener {

  protected final ApolloSubscriptionKeepAliveRunner keepAliveRunner;

  public KeepAliveSubscriptionConnectionListener() {
    this(Duration.ofSeconds(15));
  }

  public KeepAliveSubscriptionConnectionListener(Duration keepAliveInterval) {
    keepAliveRunner = new ApolloSubscriptionKeepAliveRunner(keepAliveInterval);
  }

  @Override
  public void onConnect(SubscriptionSession session, OperationMessage message) {
    keepAliveRunner.keepAlive(session);
  }

  @Override
  public void onStart(SubscriptionSession session, OperationMessage message) {
    // do nothing
  }

  @Override
  public void onStop(SubscriptionSession session, OperationMessage message) {
    // do nothing
  }

  @Override
  public void onTerminate(SubscriptionSession session, OperationMessage message) {
    keepAliveRunner.abort(session);
  }

  @Override
  public void shutdown() {
    keepAliveRunner.shutdown();
  }

}
