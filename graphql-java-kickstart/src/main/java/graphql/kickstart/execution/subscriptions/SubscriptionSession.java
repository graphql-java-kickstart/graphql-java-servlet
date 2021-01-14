package graphql.kickstart.execution.subscriptions;

import graphql.ExecutionResult;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

public interface SubscriptionSession {

  void subscribe(String id, Publisher<ExecutionResult> data);

  void add(String id, Subscription subscription);

  void unsubscribe(String id);

  void send(String message);

  void sendMessage(Object payload);

  void sendDataMessage(String id, Object payload);

  void sendErrorMessage(String id, Object payload);

  void sendCompleteMessage(String id);

  void close(String reason);

  /**
   * While the session is open, this method returns a Map that the developer may use to store
   * application specific information relating to this session instance. The developer may retrieve
   * information from this Map at any time between the opening of the session and during the
   * onClose() method. But outside that time, any information stored using this Map may no longer be
   * kept by the container. Web socket applications running on distributed implementations of the
   * web container should make any application specific objects stored here java.io.Serializable, or
   * the object may not be recreated after a failover.
   *
   * @return an editable Map of application data.
   */
  Map<String, Object> getUserProperties();

  boolean isOpen();

  String getId();

  SessionSubscriptions getSubscriptions();

  Object unwrap();

  Publisher<String> getPublisher();
}
