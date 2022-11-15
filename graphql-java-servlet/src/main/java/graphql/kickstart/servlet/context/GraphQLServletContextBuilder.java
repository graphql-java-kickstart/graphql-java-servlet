package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.execution.context.GraphQLContextBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;

public interface GraphQLServletContextBuilder extends GraphQLContextBuilder {

  GraphQLKickstartContext build(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);

  GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest);
}
