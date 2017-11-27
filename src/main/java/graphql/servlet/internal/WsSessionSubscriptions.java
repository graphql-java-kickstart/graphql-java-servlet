package graphql.servlet.internal;

import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Potter
 */
public class WsSessionSubscriptions {
    private final Object lock = new Object();

    private boolean closed = false;
    private List<Subscription> subscriptions = new ArrayList<>();

    public void add(Subscription subscription) {
        synchronized (lock) {
            if(closed) {
                throw new IllegalStateException("Websocket was already closed!");
            }
            subscriptions.add(subscription);
        }
    }

    public void cancel(Subscription subscription) {
        synchronized (lock) {
            subscriptions.remove(subscription);
            subscription.cancel();
        }
    }

    public void close() {
        synchronized (lock) {
            closed = true;
            subscriptions.forEach(Subscription::cancel);
            subscriptions = new ArrayList<>();
        }
    }
}
