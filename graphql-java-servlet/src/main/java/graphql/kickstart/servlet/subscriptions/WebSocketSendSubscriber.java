package graphql.kickstart.servlet.subscriptions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Slf4j
@RequiredArgsConstructor
public class WebSocketSendSubscriber implements Subscriber<String> {

  private final Session session;
  private AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();

  @Override
  public void onSubscribe(Subscription subscription) {
    subscriptionRef.set(subscription);
    subscriptionRef.get().request(1);
  }

  @Override
  public void onNext(String message) {
    subscriptionRef.get().request(1);
    if (session.isOpen()) {
      try {
        session.getBasicRemote().sendText(message);
      } catch (IOException e) {
        log.error("Cannot send message {}", message, e);
      }
    }
  }

  @Override
  public void onError(Throwable t) {
    log.error("WebSocket error", t);
  }

  @Override
  public void onComplete() {
    subscriptionRef.get().request(1);
    if (session.isOpen()) {
      try {
        log.debug("Closing session");
        session.close();
      } catch (IOException e) {
        log.error("Cannot close session", e);
      }
    }
    subscriptionRef.get().cancel();
  }
}
