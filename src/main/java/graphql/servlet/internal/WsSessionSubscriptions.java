package graphql.servlet.internal;

import org.reactivestreams.Subscription;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Potter
 */
public class WsSessionSubscriptions {
    private final Object lock = new Object();

    private boolean closed = false;
    private Map<String, Subscription> subscriptions = new HashMap<>();

    public void add(Subscription subscription) {
        add(getImplicitId(subscription), subscription);
    }

    public void add(String id, Subscription subscription) {
        synchronized (lock) {
            if(closed) {
                throw new IllegalStateException("Websocket was already closed!");
            }
            subscriptions.put(id, subscription);
        }
    }

    public void cancel(Subscription subscription) {
        cancel(getImplicitId(subscription));
    }

    public void cancel(String id) {
        synchronized (lock) {
            Subscription subscription = subscriptions.remove(id);
            if(subscription != null) {
                subscription.cancel();
            }
        }
    }

    public void close() {
        synchronized (lock) {
            closed = true;
            subscriptions.forEach((k, v) -> v.cancel());
            subscriptions = new HashMap<>();
        }
    }

    private String getImplicitId(Subscription subscription) {
        return String.valueOf(subscription.hashCode());
    }
}
