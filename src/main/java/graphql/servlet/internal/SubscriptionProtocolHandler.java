package graphql.servlet.internal;

import graphql.ExecutionResult;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Andrew Potter
 */
public abstract class SubscriptionProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionProtocolHandler.class);

    public abstract void onMessage(HandshakeRequest request, Session session, WsSessionSubscriptions subscriptions, String text) throws Exception;

    protected abstract void sendDataMessage(Session session, String id, Object payload);

    protected abstract void sendErrorMessage(Session session, String id);

    protected abstract void sendCompleteMessage(Session session, String id);

    protected void subscribe(Session session, ExecutionResult executionResult, WsSessionSubscriptions subscriptions, String id) {
        final Object data = executionResult.getData();

        if (data instanceof Publisher) {
            @SuppressWarnings("unchecked") final Publisher<ExecutionResult> publisher = (Publisher<ExecutionResult>) data;
            final AtomicSubscriptionReference subscriptionReference = new AtomicSubscriptionReference();

            publisher.subscribe(new Subscriber<ExecutionResult>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriptionReference.set(subscription);
                    subscriptionReference.get().request(1);

                    subscriptions.add(id, subscriptionReference.get());
                }

                @Override
                public void onNext(ExecutionResult executionResult) {
                    subscriptionReference.get().request(1);
                    Map<String, Object> result = new HashMap<>();
                    result.put("data", executionResult.getData());
                    sendDataMessage(session, id, result);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Subscription error", throwable);
                    subscriptions.cancel(id);
                    sendErrorMessage(session, id);
                }

                @Override
                public void onComplete() {
                    subscriptions.cancel(id);
                    sendCompleteMessage(session, id);
                }
            });
        }
    }

    protected void unsubscribe(WsSessionSubscriptions subscriptions, String id) {
        subscriptions.cancel(id);
    }

    static class AtomicSubscriptionReference {
        private final AtomicReference<Subscription> reference = new AtomicReference<>(null);

        public void set(Subscription subscription) {
            if(reference.get() != null) {
                throw new IllegalStateException("Cannot overwrite subscription!");
            }

            reference.set(subscription);
        }

        public Subscription get() {
            Subscription subscription = reference.get();
            if(subscription == null) {
                throw new IllegalStateException("Subscription has not been initialized yet!");
            }

            return subscription;
        }
    }
}
