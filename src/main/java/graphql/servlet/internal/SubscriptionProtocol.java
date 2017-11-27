package graphql.servlet.internal;

import javax.websocket.MessageHandler;
import javax.websocket.server.HandshakeRequest;
import java.util.function.Function;

/**
 * @author Andrew Potter
 */
public interface SubscriptionProtocol extends MessageHandler.Whole<String> {
    void onMessage(HandshakeRequest request, String text, Function query);
}
