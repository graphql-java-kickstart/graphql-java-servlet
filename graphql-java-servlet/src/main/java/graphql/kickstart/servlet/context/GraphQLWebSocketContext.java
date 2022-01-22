package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLWebSocketContext extends GraphQLKickstartContext {

  Session getSession();

  HandshakeRequest getHandshakeRequest();
}
