package graphql.kickstart.execution.subscriptions;

public class SubscriptionException extends Exception {

  private final transient Object payload;

  public SubscriptionException() {
    this(null);
  }

  public SubscriptionException(Object payload) {
    this.payload = payload;
  }

  public Object getPayload() {
    return payload;
  }
}
