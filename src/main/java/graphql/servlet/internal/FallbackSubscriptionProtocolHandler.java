package graphql.servlet.internal;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.io.IOException;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolHandler extends SubscriptionProtocolHandler {

    private final SubscriptionHandlerInput input;

    public FallbackSubscriptionProtocolHandler(SubscriptionHandlerInput subscriptionHandlerInput) {
        this.input = subscriptionHandlerInput;
    }

    @Override
    public void onMessage(HandshakeRequest request, Session session, WsSessionSubscriptions subscriptions, String text) throws Exception {
        subscribe(session, input.getQueryInvoker().query(input.getInvocationInputFactory().create(
                input.getGraphQLObjectMapper().readGraphQLRequest(text))), subscriptions, session.getId());
    }

    @Override
    protected void sendDataMessage(Session session, String id, Object payload) {
        try {
            session.getBasicRemote().sendText(input.getGraphQLObjectMapper().getJacksonMapper().writeValueAsString(payload));
        } catch (IOException e) {
            throw new RuntimeException("Error sending subscription response", e);
        }
    }

    @Override
    protected void sendErrorMessage(Session session, String id) {

    }

    @Override
    protected void sendCompleteMessage(Session session, String id) {

    }
}
