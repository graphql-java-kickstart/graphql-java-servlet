package graphql.kickstart.servlet.core;

import graphql.kickstart.execution.StaticGraphQLRootObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public class DefaultGraphQLRootObjectBuilder extends StaticGraphQLRootObjectBuilder implements
    GraphQLServletRootObjectBuilder {

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
