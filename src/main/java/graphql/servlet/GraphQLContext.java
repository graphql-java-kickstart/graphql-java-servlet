package graphql.servlet;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphQLContext {
    private HttpServletRequest httpServletRequest;
    private HandshakeRequest handshakeRequest;

    private Subject subject;
    private Map<String, List<Part>> files;

    public GraphQLContext(HttpServletRequest httpServletRequest, HandshakeRequest handshakeRequest, Subject subject) {
        this.httpServletRequest = httpServletRequest;
        this.handshakeRequest = handshakeRequest;
        this.subject = subject;
    }

    public Optional<HttpServletRequest> getHttpServletRequest() {
        return Optional.ofNullable(httpServletRequest);
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
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
}
