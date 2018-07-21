package graphql.servlet.internal;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

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
        session.getBasicRemote().sendText(input.getGraphQLObjectMapper().serializeResultAsJson(
            input.getQueryInvoker().query(input.getInvocationInputFactory().create(input.getGraphQLObjectMapper().readGraphQLRequest(text), request))
        ));
    }
}
