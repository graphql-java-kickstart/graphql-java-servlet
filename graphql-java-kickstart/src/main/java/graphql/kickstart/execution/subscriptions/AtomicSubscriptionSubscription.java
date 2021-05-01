package graphql.kickstart.execution.subscriptions;

import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscription;

public class AtomicSubscriptionSubscription {

  private final AtomicReference<Subscription> reference = new AtomicReference<>(null);

  public void set(Subscription subscription) {
    if (reference.get() != null) {
      throw new IllegalStateException("Cannot overwrite subscription!");
    }

    reference.set(subscription);
  }

  public Subscription get() {
    Subscription subscription = reference.get();
    if (subscription == null) {
      throw new IllegalStateException("Subscription has not been initialized yet!");
    }

    return subscription;
  }
}
