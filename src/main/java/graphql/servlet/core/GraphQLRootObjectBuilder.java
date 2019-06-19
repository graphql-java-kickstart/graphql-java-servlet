package graphql.servlet.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.HandshakeRequest;
import java.util.Optional;

public interface GraphQLRootObjectBuilder {
    Object build(HttpServletRequest req);
    Object build(HandshakeRequest req);

    /**
     * Only used for MBean calls.
     * @return the graphql root object
     */
    Object build();
}
