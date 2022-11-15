package graphql.kickstart.servlet.core;

import graphql.kickstart.execution.GraphQLRootObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.HandshakeRequest;

public interface GraphQLServletRootObjectBuilder extends GraphQLRootObjectBuilder {

  Object build(HttpServletRequest req);

  Object build(HandshakeRequest req);
}
