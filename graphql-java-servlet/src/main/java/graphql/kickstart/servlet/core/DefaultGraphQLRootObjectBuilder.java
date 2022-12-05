package graphql.kickstart.servlet.core;

import graphql.kickstart.execution.StaticGraphQLRootObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.HandshakeRequest;

public class DefaultGraphQLRootObjectBuilder extends StaticGraphQLRootObjectBuilder
    implements GraphQLServletRootObjectBuilder {

  public DefaultGraphQLRootObjectBuilder() {
    super(new Object());
  }

  @Override
  public Object build(HttpServletRequest req) {
    return getRootObject();
  }

  @Override
  public Object build(HandshakeRequest req) {
    return getRootObject();
  }
}
