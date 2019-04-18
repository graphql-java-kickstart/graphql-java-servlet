package graphql.servlet.internal;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolHandler extends SubscriptionProtocolHandler {

    private final SubscriptionHandlerInput input;
    private final SubscriptionSender sender;

    public FallbackSubscriptionProtocolHandler(SubscriptionHandlerInput subscriptionHandlerInput) {
        this.input = subscriptionHandlerInput;
        sender = new SubscriptionSender(subscriptionHandlerInput.getGraphQLObjectMapper().getJacksonMapper());
    }

    @Override
    public void onMessage(HandshakeRequest request, Session session, WsSessionSubscriptions subscriptions, String text) throws Exception {
        subscribe(
            session,
            input.getQueryInvoker().query(
                input.getInvocationInputFactory().create(
                    input.getGraphQLObjectMapper().readGraphQLRequest(text)
                )
            ),
            subscriptions,
            UUID.randomUUID().toString()
        );
    }

    @Override
    protected void sendDataMessage(Session session, String id, Object payload) {
        sender.send(session, payload);
    }

    @Override
    protected void sendErrorMessage(Session session, String id) {

    }

    @Override
    protected void sendCompleteMessage(Session session, String id) {

    }
}
