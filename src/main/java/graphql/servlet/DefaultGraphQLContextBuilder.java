package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.HandshakeRequest;

public class DefaultGraphQLContextBuilder implements GraphQLContextBuilder {

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new GraphQLContext(httpServletRequest, httpServletResponse);
    }

    @Override
    public GraphQLContext build(HandshakeRequest handshakeRequest) {
        return new GraphQLContext(handshakeRequest);
    }

    @Override
    public GraphQLContext build() {
        return new GraphQLContext();
    }
}
