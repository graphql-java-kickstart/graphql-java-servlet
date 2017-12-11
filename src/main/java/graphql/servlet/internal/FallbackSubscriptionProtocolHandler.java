package graphql.servlet.internal;

import javax.websocket.server.HandshakeRequest;
import java.util.function.Function;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolHandler implements SubscriptionProtocolHandler {
    @Override
    public void onMessage(HandshakeRequest request, String text, Function query) {

    }
}
