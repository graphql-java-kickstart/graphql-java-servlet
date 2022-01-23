package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.execution.context.GraphQLContextBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLServletContextBuilder extends GraphQLContextBuilder {

  GraphQLKickstartContext build(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);

  GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest);
}
