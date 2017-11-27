package graphql.servlet;

import org.apache.commons.fileupload.FileItem;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphQLContext {
    private final HttpServletRequest httpServletRequest;
    private final HandshakeRequest handshakeRequest;
    private final Subject subject;

    private Map<String, List<FileItem>> files = null;

    public GraphQLContext(HttpServletRequest httpServletRequest, HandshakeRequest handshakeRequest, Subject subject) {
        this.httpServletRequest = httpServletRequest;
        this.handshakeRequest = handshakeRequest;
        this.subject = subject;
    }

    public GraphQLContext(HttpServletRequest httpServletRequest) {
        this(httpServletRequest, null, null);
    }

    public GraphQLContext(HandshakeRequest handshakeRequest) {
        this(null, handshakeRequest, null);
    }

    public GraphQLContext() {
        this(null, null, null);
    }

    public Optional<HttpServletRequest> getHttpServletRequest() {
        return Optional.ofNullable(httpServletRequest);
    }

    public Optional<HandshakeRequest> getHandshakeRequest() {
        return Optional.ofNullable(handshakeRequest);
    }

    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<Map<String, List<FileItem>>> getFiles() {
        return Optional.ofNullable(files);
    }

    public void setFiles(Map<String, List<FileItem>> files) {
        this.files = files;
    }
}
