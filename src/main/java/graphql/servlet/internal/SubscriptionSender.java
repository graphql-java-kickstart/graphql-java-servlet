package graphql.servlet.internal;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.Session;
import java.io.IOException;

class SubscriptionSender {

    private final ObjectMapper objectMapper;

    SubscriptionSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void send(Session session, Object payload) {
        try {
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new RuntimeException("Error sending subscription response", e);
        }
    }
}
