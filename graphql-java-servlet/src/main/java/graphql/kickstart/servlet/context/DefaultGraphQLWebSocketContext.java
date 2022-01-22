package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import java.util.HashMap;
import java.util.Map;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import org.dataloader.DataLoaderRegistry;

public class DefaultGraphQLWebSocketContext extends DefaultGraphQLContext
    implements GraphQLWebSocketContext {

  private final Session session;
  private final HandshakeRequest handshakeRequest;

  private DefaultGraphQLWebSocketContext(
      DataLoaderRegistry dataLoaderRegistry, Session session, HandshakeRequest handshakeRequest) {
    super(dataLoaderRegistry);
    this.session = session;
    this.handshakeRequest = handshakeRequest;
  }

  public static Builder createWebSocketContext(DataLoaderRegistry registry) {
    return new Builder(registry);
  }

  public static Builder createWebSocketContext() {
    return new Builder(new DataLoaderRegistry());
  }

  @Override
  public Session getSession() {
    return session;
  }

  @Override
  public HandshakeRequest getHandshakeRequest() {
    return handshakeRequest;
  }

  @Override
  public Map<Object, Object> getMapOfContext() {
    Map<Object, Object> map = new HashMap<>();
    map.put(DataLoaderRegistry.class, getDataLoaderRegistry());
    map.put(Session.class, session);
    map.put(HandshakeRequest.class, handshakeRequest);
    return map;
  }

  public static class Builder {

    private Session session;
    private HandshakeRequest handshakeRequest;
    private DataLoaderRegistry dataLoaderRegistry;

    private Builder(DataLoaderRegistry dataLoaderRegistry) {
      this.dataLoaderRegistry = dataLoaderRegistry;
    }

    public DefaultGraphQLWebSocketContext build() {
      return new DefaultGraphQLWebSocketContext(dataLoaderRegistry, session, handshakeRequest);
    }

    public Builder with(Session session) {
      this.session = session;
      return this;
    }

    public Builder with(HandshakeRequest handshakeRequest) {
      this.handshakeRequest = handshakeRequest;
      return this;
    }

    public Builder with(DataLoaderRegistry dataLoaderRegistry) {
      this.dataLoaderRegistry = dataLoaderRegistry;
      return this;
    }
  }
}
