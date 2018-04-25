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

    abstract void onMessage(HandshakeRequest request, Session session, WsSessionSubscriptions subscriptions, String text) throws Exception;

    protected void subscribe(ExecutionResult executionResult, WsSessionSubscriptions subscriptions, String id) {
        final Object data = executionResult.getData();

        if(data instanceof Publisher) {
            @SuppressWarnings("unchecked")
            final Publisher<ExecutionResult> publisher = (Publisher<ExecutionResult>) data;
            final AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();

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
                    sendMessage(session, ApolloSubscriptionProtocolHandler.OperationMessage.Type.GQL_DATA, id, result);
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Subscription error", throwable);
                    subscriptions.cancel(id);
                    sendMessage(session, ApolloSubscriptionProtocolHandler.OperationMessage.Type.GQL_ERROR, id);
                }

                @Override
                public void onComplete() {
                    subscriptions.cancel(id);
                    sendMessage(session, ApolloSubscriptionProtocolHandler.OperationMessage.Type.GQL_COMPLETE, id);
                }
            });
        }
        }
    }

    public static
}
