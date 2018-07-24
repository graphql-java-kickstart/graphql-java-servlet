package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public class DefaultGraphQLContextBuilder implements GraphQLContextBuilder {

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest) {
        return new GraphQLContext(httpServletRequest);
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
