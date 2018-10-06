package graphql.servlet;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphQLContext {

    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private Session session;
    private HandshakeRequest handshakeRequest;

    private Subject subject;
    private Map<String, List<Part>> files;

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

    public Optional<HandshakeRequest> getHandshakeRequest() {
        return Optional.ofNullable(handshakeRequest);
    }

    public Optional<Map<String, List<Part>>> getFiles() {
        return Optional.ofNullable(files);
    }

    public void setFiles(Map<String, List<Part>> files) {
        this.files = files;
    }

    public Optional<DataLoaderRegistry> getDataLoaderRegistry() {
        return Optional.ofNullable(dataLoaderRegistry);
    }

    public void setDataLoaderRegistry(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }
}
