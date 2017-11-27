package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.HandshakeRequest;
import java.util.Optional;

public interface GraphQLContextBuilder {
    GraphQLContext build(HttpServletRequest httpServletRequest);
    GraphQLContext build(HandshakeRequest handshakeRequest);

    /**
     * Only used for MBean calls.
     * @return the graphql context
     */
    GraphQLContext build();
}
