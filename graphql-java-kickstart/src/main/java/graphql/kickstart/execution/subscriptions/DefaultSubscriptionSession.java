package graphql.kickstart.execution.subscriptions;

import graphql.ExecutionResult;
import graphql.execution.reactive.SingleSubscriberPublisher;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

@Slf4j
@RequiredArgsConstructor
public class DefaultSubscriptionSession implements SubscriptionSession {

  @Getter private final GraphQLSubscriptionMapper mapper;
  private SingleSubscriberPublisher<String> publisher = new SingleSubscriberPublisher<>();
  private SessionSubscriptions subscriptions = new SessionSubscriptions();

  @Override
  public void send(String message) {
    Objects.requireNonNull(message, "message is required");
    publisher.offer(message);
  }

  @Override
  public void sendMessage(Object payload) {
    Objects.requireNonNull(payload, "payload is required");
    send(mapper.serialize(payload));
  }

  @Override
  public void subscribe(String id, Publisher<ExecutionResult> dataPublisher) {
    dataPublisher.subscribe(new SessionSubscriber(this, id));
  }

  @Override
  public void add(String id, Subscription subscription) {
    subscriptions.add(id, subscription);
  }

  @Override
  public void unsubscribe(String id) {
    subscriptions.cancel(id);
  }

  @Override
  public void sendDataMessage(String id, Object payload) {
    send(mapper.serialize(payload));
  }

  @Override
  public void sendErrorMessage(String id, Object payload) {
    send(mapper.serialize(payload));
  }

  @Override
  public void sendCompleteMessage(String id) {
    // default empty implementation
  }

  @Override
  public void close(String reason) {
    log.debug("Closing subscription session {}", getId());
    subscriptions.close();
    publisher.noMoreData();
  }

  @Override
  public Map<String, Object> getUserProperties() {
    return new HashMap<>();
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public SessionSubscriptions getSubscriptions() {
    return subscriptions;
  }

  @Override
  public Object unwrap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Publisher<String> getPublisher() {
    return publisher;
  }

  @Override
  public String toString() {
    return getId();
  }
}
