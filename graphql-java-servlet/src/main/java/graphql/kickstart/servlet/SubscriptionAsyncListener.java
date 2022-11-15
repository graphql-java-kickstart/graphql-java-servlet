package graphql.kickstart.servlet;

import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
class SubscriptionAsyncListener implements AsyncListener {

  private final AtomicReference<Subscription> subscriptionRef;

  @Override
  public void onComplete(AsyncEvent event) {
    subscriptionRef.get().cancel();
  }

  @Override
  public void onTimeout(AsyncEvent event) {
    subscriptionRef.get().cancel();
  }

  @Override
  public void onError(AsyncEvent event) {
    subscriptionRef.get().cancel();
  }

  @Override
  public void onStartAsync(AsyncEvent event) {
    // default empty implementation
  }
}
