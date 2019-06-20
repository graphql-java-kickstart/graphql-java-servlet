package graphql.servlet.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;

public interface GraphQLContextBuilder {

    GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Map<String, List<Part>> fileParts);

    GraphQLContext build(Session session, HandshakeRequest handshakeRequest);

    /**
     * Only used for MBean calls.
     * @return the graphql context
     */
    GraphQLContext build();
}
