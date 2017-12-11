package graphql.servlet.internal;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

/**
 * @author Andrew Potter
 */
public class ApolloSubscriptionProtocolHandler implements SubscriptionProtocolHandler {
    @Override
    public void onMessage(HandshakeRequest request, Session session, String text) {

    }
}
