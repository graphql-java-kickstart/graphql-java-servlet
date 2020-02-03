package graphql.kickstart.servlet.core;

import graphql.kickstart.execution.GraphQLRootObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLServletRootObjectBuilder extends GraphQLRootObjectBuilder {

  Object build(HttpServletRequest req);

  Object build(HandshakeRequest req);

}
