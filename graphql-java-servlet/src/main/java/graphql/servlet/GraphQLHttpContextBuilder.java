package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

public class GraphQLHttpContextBuilder implements GraphQLContextBuilder {

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new GraphQLHttpServletContext(httpServletRequest, httpServletResponse);
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        return new GraphQLWebsocketContext(session, handshakeRequest);
    }

    @Override
    public GraphQLContext build() {
        return new GraphQLHttpContext();
    }

}
