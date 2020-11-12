package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import javax.security.auth.Subject;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import org.dataloader.DataLoaderRegistry;

import java.util.Locale;

public class DefaultGraphQLWebSocketContext extends DefaultGraphQLContext implements GraphQLWebSocketContext {

  private final Session session;
  private final HandshakeRequest handshakeRequest;

  private DefaultGraphQLWebSocketContext(DataLoaderRegistry dataLoaderRegistry, Subject subject,
                                         Locale locale, Session session, HandshakeRequest handshakeRequest) {
    super(dataLoaderRegistry, subject,locale);
    this.session = session;
    this.handshakeRequest = handshakeRequest;
  }

  public static Builder createWebSocketContext(DataLoaderRegistry registry, Subject subject) {
    return new Builder(registry, subject);
  }

  public static Builder createWebSocketContext() {
    return new Builder(new DataLoaderRegistry(), null);
  }

  @Override
  public Session getSession() {
    return session;
  }

  @Override
  public HandshakeRequest getHandshakeRequest() {
    return handshakeRequest;
  }

  public static class Builder {

    private Session session;
    private HandshakeRequest handshakeRequest;
    private DataLoaderRegistry dataLoaderRegistry;
    private Subject subject;
    private Locale locale;

    private Builder(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
      this.dataLoaderRegistry = dataLoaderRegistry;
      this.subject = subject;
    }

    public DefaultGraphQLWebSocketContext build() {
      return new DefaultGraphQLWebSocketContext(dataLoaderRegistry, subject, locale, session, handshakeRequest);
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

    public Builder with(Subject subject) {
      this.subject = subject;
      return this;
    }

    public Builder with(Locale locale) {
      this.locale = locale;
      return this;
    }
  }

}
