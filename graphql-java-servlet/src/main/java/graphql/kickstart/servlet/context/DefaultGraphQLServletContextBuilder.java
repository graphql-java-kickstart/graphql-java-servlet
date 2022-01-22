package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContextBuilder;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

/** Returns an empty context. */
public class DefaultGraphQLServletContextBuilder extends DefaultGraphQLContextBuilder
    implements GraphQLServletContextBuilder {

  @Override
  public GraphQLKickstartContext build(HttpServletRequest request, HttpServletResponse response) {
    return DefaultGraphQLServletContext.createServletContext().with(request).with(response).build();
  }

  @Override
  public GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
    return DefaultGraphQLWebSocketContext.createWebSocketContext()
        .with(session)
        .with(handshakeRequest)
        .build();
  }
}
