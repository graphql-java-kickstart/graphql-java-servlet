package graphql.servlet.context;

import graphql.servlet.core.ApolloSubscriptionConnectionListener;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultGraphQLWebSocketContext extends DefaultGraphQLServletContext implements GraphQLWebSocketContext {

    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final Session session;
    private final HandshakeRequest handshakeRequest;
    private final Map<String, List<Part>> parts;

    private DefaultGraphQLWebSocketContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, HttpServletRequest httpServletRequest,
                                          HttpServletResponse httpServletResponse, Session session, HandshakeRequest handshakeRequest,
                                           Map<String, List<Part>> parts) {
        super(dataLoaderRegistry, subject);
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.session = session;
        this.handshakeRequest = handshakeRequest;
        this.parts = parts;
    }

    @Override
    public Optional<HttpServletRequest> getHttpServletRequest() {
        return Optional.ofNullable(httpServletRequest);
    }

    @Override
    public Optional<HttpServletResponse> getHttpServletResponse() {
        return Optional.ofNullable(httpServletResponse);
    }

    @Override
    public List<Part> getFileParts() {
        return parts.values()
            .stream()
            .flatMap(Collection::stream)
            .filter(part -> part.getContentType() != null)
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Part>> getParts() {
        return parts;
    }

    @Override
    public Optional<Session> getSession() {
        return Optional.ofNullable(session);
    }

    @Override
    public Optional<Object> getConnectResult() {
        return getSession().map(session -> session.getUserProperties().get(ApolloSubscriptionConnectionListener.CONNECT_RESULT_KEY));
    }

    @Override
    public Optional<HandshakeRequest> getHandshakeRequest() {
        return Optional.ofNullable(handshakeRequest);
    }

    public static Builder createWebContext(DataLoaderRegistry registry, Subject subject) {
        return new Builder(registry, subject);
    }

    public static Builder createWebContext() {
        return new Builder(null, null);
    }

    public static class Builder {
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;
        private Session session;
        private HandshakeRequest handshakeRequest;
        private Map<String, List<Part>> parts = new HashMap<>();
        private DataLoaderRegistry dataLoaderRegistry;
        private Subject subject;

        private Builder(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
            this.dataLoaderRegistry = dataLoaderRegistry;
            this.subject = subject;
        }

        public DefaultGraphQLWebSocketContext build() {
            return new DefaultGraphQLWebSocketContext(dataLoaderRegistry, subject, httpServletRequest, httpServletResponse, session, handshakeRequest, parts);
        }

        public Builder with(HttpServletRequest httpServletRequest) {
            this.httpServletRequest = httpServletRequest;
            return this;
        }

        public Builder with(HttpServletResponse httpServletResponse) {
            this.httpServletResponse = httpServletResponse;
            return this;
        }

        public Builder with(Session session) {
            this.session = session;
            return this;
        }

        public Builder with(HandshakeRequest handshakeRequest) {
            this.handshakeRequest = handshakeRequest;
            return this;
        }

        public Builder with(Map<String, List<Part>> parts) {
            if (parts != null) {
                this.parts.putAll(parts);
            }
            return this;
        }
    }
}
