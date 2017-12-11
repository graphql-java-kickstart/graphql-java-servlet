package graphql.servlet.internal;

import javax.websocket.server.HandshakeRequest;
import java.util.function.Function;

/**
 * @author Andrew Potter
 */
public interface SubscriptionProtocolHandler {
    void onMessage(HandshakeRequest request, String text, Function query);
}
