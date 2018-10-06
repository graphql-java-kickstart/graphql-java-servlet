package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLContextBuilder {

    GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);

    GraphQLContext build(HandshakeRequest handshakeRequest);

    /**
     * Only used for MBean calls.
     * @return the graphql context
     */
    GraphQLContext build();
}
