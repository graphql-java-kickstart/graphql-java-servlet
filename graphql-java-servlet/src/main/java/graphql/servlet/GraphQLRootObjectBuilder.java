package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLRootObjectBuilder {

    Object build(HttpServletRequest req);

    Object build(HandshakeRequest req);

    /**
     * Only used for MBean calls.
     *
     * @return the graphql root object
     */
    Object build();

}
