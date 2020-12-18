package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.GraphQLContext;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

public interface GraphQLWebSocketContext extends GraphQLContext {

  Session getSession();

  HandshakeRequest getHandshakeRequest();

}
