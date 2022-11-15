package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContextBuilder;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;

/** Returns an empty context. */
public class DefaultGraphQLServletContextBuilder extends DefaultGraphQLContextBuilder
    implements GraphQLServletContextBuilder {

  @Override
  public GraphQLKickstartContext build(HttpServletRequest request, HttpServletResponse response) {
    Map<Object, Object> map = new HashMap<>();
    map.put(HttpServletRequest.class, request);
    map.put(HttpServletResponse.class, response);
    return GraphQLKickstartContext.of(map);
  }

  @Override
  public GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
    Map<Object, Object> map = new HashMap<>();
    map.put(Session.class, session);
    map.put(HandshakeRequest.class, handshakeRequest);
    return GraphQLKickstartContext.of(map);
  }
}
