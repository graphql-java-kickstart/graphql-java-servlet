package graphql.kickstart.servlet.subscriptions;

import graphql.kickstart.execution.subscriptions.DefaultSubscriptionSession;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import java.util.Map;
import jakarta.websocket.Session;

public class WebSocketSubscriptionSession extends DefaultSubscriptionSession {

  private final Session session;

  public WebSocketSubscriptionSession(GraphQLSubscriptionMapper mapper, Session session) {
    super(mapper);
    this.session = session;
  }

  @Override
  public boolean isOpen() {
    return session.isOpen();
  }

  @Override
  public Map<String, Object> getUserProperties() {
    return session.getUserProperties();
  }

  @Override
  public String getId() {
    return session.getId();
  }

  @Override
  public Session unwrap() {
    return session;
  }
}
