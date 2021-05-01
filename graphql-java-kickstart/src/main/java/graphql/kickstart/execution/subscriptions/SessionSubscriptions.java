package graphql.kickstart.execution.subscriptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.reactivestreams.Subscription;

/** @author Andrew Potter */
public class SessionSubscriptions {

  private final Object lock = new Object();

  private boolean closed = false;
  private Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

  public void add(Subscription subscription) {
    add(getImplicitId(subscription), subscription);
  }

  public void add(String id, Subscription subscription) {
    synchronized (lock) {
      if (closed) {
        throw new IllegalStateException("Websocket was already closed!");
      }
      subscriptions.put(id, subscription);
    }
  }

  public void cancel(Subscription subscription) {
    cancel(getImplicitId(subscription));
  }

  public void cancel(String id) {
    Subscription subscription = subscriptions.remove(id);
    if (subscription != null) {
      subscription.cancel();
    }
  }

  public void close() {
    synchronized (lock) {
      closed = true;
      subscriptions.forEach((k, v) -> v.cancel());
      subscriptions.clear();
    }
  }

  private String getImplicitId(Subscription subscription) {
    return String.valueOf(subscription.hashCode());
  }

  public int getSubscriptionCount() {
    return subscriptions.size();
  }
}
