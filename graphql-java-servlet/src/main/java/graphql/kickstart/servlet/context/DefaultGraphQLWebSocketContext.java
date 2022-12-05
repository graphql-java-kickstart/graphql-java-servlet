package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import org.dataloader.DataLoaderRegistry;

/** @deprecated Use {@link graphql.kickstart.execution.context.GraphQLKickstartContext} instead */
@Deprecated
public class DefaultGraphQLWebSocketContext extends DefaultGraphQLContext
    implements GraphQLWebSocketContext {

  private DefaultGraphQLWebSocketContext(
      DataLoaderRegistry dataLoaderRegistry, Session session, HandshakeRequest handshakeRequest) {
    super(dataLoaderRegistry);
    put(Session.class, session);
    put(HandshakeRequest.class, handshakeRequest);
  }

  public static Builder createWebSocketContext(DataLoaderRegistry registry) {
    return new Builder(registry);
  }

  public static Builder createWebSocketContext() {
    return new Builder(new DataLoaderRegistry());
  }

  /**
   * @deprecated Use {@code dataFetchingEnvironment.getGraphQlContext().get(Session.class)} instead.
   *     Since 13.0.0
   */
  @Override
  @Deprecated
  public Session getSession() {
    return (Session) getMapOfContext().get(Session.class);
  }

  /**
   * @deprecated Use {@code dataFetchingEnvironment.getGraphQlContext().get(HandshakeRequest.class)}
   *     instead. Since 13.0.0
   */
  @Override
  @Deprecated
  public HandshakeRequest getHandshakeRequest() {
    return (HandshakeRequest) getMapOfContext().get(HandshakeRequest.class);
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
