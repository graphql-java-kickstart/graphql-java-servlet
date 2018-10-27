package graphql.servlet;

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

public class GraphQLContext {

    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private Session session;
    private HandshakeRequest handshakeRequest;

    private Subject subject;
    private Map<String, List<Part>> parts;

    private DataLoaderRegistry dataLoaderRegistry;

    public GraphQLContext(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Session session, HandshakeRequest handshakeRequest, Subject subject) {
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.session = session;
        this.handshakeRequest = handshakeRequest;
        this.subject = subject;
    }

    public GraphQLContext(HttpServletRequest httpServletRequest) {
        this(httpServletRequest, null);
    }

    public GraphQLContext(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this(httpServletRequest, httpServletResponse, null, null, null);
    }

    public GraphQLContext(Session session, HandshakeRequest handshakeRequest) {
        this(null, null, session, handshakeRequest, null);
    }

    public GraphQLContext() {
        this(null, null, null, null, null);
    }

    public Optional<HttpServletRequest> getHttpServletRequest() {
        return Optional.ofNullable(httpServletRequest);
    }

    public Optional<HttpServletResponse> getHttpServletResponse() {
        return Optional.ofNullable(httpServletResponse);
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<Session> getSession() {
        return Optional.ofNullable(session);
    }

    public Optional<Object> getConnectResult() {
        if (session != null) {
            return Optional.ofNullable(session.getUserProperties().get(ApolloSubscriptionConnectionListener.CONNECT_RESULT_KEY));
        }
        return Optional.empty();
    }

    public Optional<HandshakeRequest> getHandshakeRequest() {
        return Optional.ofNullable(handshakeRequest);
    }

    /**
     * @return list of all parts representing files
     */
    public List<Part> getFileParts() {
        return getParts().values()
                .stream()
                .flatMap(Collection::stream)
                .filter(part -> part.getContentType() != null)
                .collect(Collectors.toList());
    }

    /**
     * Contrary what the name implies this method returns all parts and not just the ones that represent actual files.
     * That's why this method has been deprecated in favor of the ones that communicate their intent more clearly.
     *
     * @deprecated use {@link #getParts()} or {@link #getFileParts()} instead
     */
    @Deprecated
    public Optional<Map<String, List<Part>>> getFiles() {
        return Optional.ofNullable(parts);
    }

    /**
     * @return map representing all form fields
     */
    public Map<String, List<Part>> getParts() {
        return parts != null ? parts : new HashMap<>();
    }

    public void setParts(Map<String, List<Part>> parts) {
        this.parts = parts;
    }

    public Optional<DataLoaderRegistry> getDataLoaderRegistry() {
        return Optional.ofNullable(dataLoaderRegistry);
    }

    public void setDataLoaderRegistry(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }
}
