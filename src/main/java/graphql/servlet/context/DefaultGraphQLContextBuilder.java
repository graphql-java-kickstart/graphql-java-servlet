package graphql.servlet.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;

/**
 * Returns an empty context.
 */
public class DefaultGraphQLContextBuilder implements GraphQLContextBuilder {

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Map<String, List<Part>> fileParts) {
        return DefaultGraphQLServletContext.createWebContext().with(httpServletRequest).with(httpServletResponse).with(fileParts).build();
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        return DefaultGraphQLWebSocketContext.createWebContext().with(session).with(handshakeRequest).build();
    }

    @Override
    public GraphQLContext build() {
        return new DefaultGraphQLContext();
    }
}
