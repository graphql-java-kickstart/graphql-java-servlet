package graphql.servlet.core.internal;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.Session;
import java.io.IOException;
import java.io.UncheckedIOException;

class SubscriptionSender {

    private final ObjectMapper objectMapper;

    SubscriptionSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void send(Session session, Object payload) {
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new UncheckedIOException("Error sending subscription response", e);
        }
    }
}
