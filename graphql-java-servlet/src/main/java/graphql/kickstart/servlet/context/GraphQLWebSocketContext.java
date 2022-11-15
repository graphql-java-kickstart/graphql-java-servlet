package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;

/** @deprecated Use {@link graphql.kickstart.execution.context.GraphQLKickstartContext} instead */
public interface GraphQLWebSocketContext extends GraphQLKickstartContext {

  Session getSession();

  HandshakeRequest getHandshakeRequest();
}
