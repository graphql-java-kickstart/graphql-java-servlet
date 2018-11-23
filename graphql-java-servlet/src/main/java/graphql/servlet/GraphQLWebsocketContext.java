package graphql.servlet;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.Optional;

public class GraphQLWebsocketContext extends GraphQLHttpContext {

    private Session session;
    private HandshakeRequest handshakeRequest;

    GraphQLWebsocketContext(Session session, HandshakeRequest handshakeRequest) {
        this.session = session;
        this.handshakeRequest = handshakeRequest;
    }

    public Optional<Session> getSession() {
        return Optional.ofNullable(session);
    }

    public Optional<Object> getConnectResult() {
        if (session != null) {
            return Optional.ofNullable(session.getUserProperties().get(ApolloSubscriptionConnectionListener.CONNECT_RESULT_KEY));
        }
        return Optional.empty();
    }

    public Optional<HandshakeRequest> getHandshakeRequest() {
        return Optional.ofNullable(handshakeRequest);
    }

}
