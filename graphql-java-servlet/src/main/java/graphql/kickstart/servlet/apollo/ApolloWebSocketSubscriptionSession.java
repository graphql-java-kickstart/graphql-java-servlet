package graphql.kickstart.servlet.apollo;

import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.apollo.ApolloSubscriptionSession;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionSession;
import java.util.Map;
import jakarta.websocket.Session;

public class ApolloWebSocketSubscriptionSession extends ApolloSubscriptionSession {

  private final WebSocketSubscriptionSession webSocketSubscriptionSession;

  public ApolloWebSocketSubscriptionSession(GraphQLSubscriptionMapper mapper, Session session) {
    super(mapper);
    webSocketSubscriptionSession = new WebSocketSubscriptionSession(mapper, session);
  }

  @Override
  public boolean isOpen() {
    return webSocketSubscriptionSession.isOpen();
  }

  @Override
  public Map<String, Object> getUserProperties() {
    return webSocketSubscriptionSession.getUserProperties();
  }

  @Override
  public String getId() {
    return webSocketSubscriptionSession.getId();
  }

  @Override
  public Session unwrap() {
    return webSocketSubscriptionSession.unwrap();
  }
}
