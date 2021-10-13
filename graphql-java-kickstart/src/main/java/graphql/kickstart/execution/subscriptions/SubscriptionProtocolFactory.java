package graphql.kickstart.execution.subscriptions;

import java.util.function.Consumer;

/** @author Andrew Potter */
public abstract class SubscriptionProtocolFactory {

  private final String protocol;

  protected SubscriptionProtocolFactory(String protocol) {
    this.protocol = protocol;
  }

  public String getProtocol() {
    return protocol;
  }

  public abstract Consumer<String> createConsumer(SubscriptionSession session);

  public void shutdown() {
    // do nothing
  }
}
