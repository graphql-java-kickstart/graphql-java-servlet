package graphql.servlet.internal;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

/**
 * @author Andrew Potter
 */
public interface SubscriptionProtocolHandler {
    void onMessage(HandshakeRequest request, Session session, String text) throws Exception;
}
