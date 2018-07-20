package graphql.servlet;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GraphQLContext {
    private Optional<HttpServletRequest> request;
    private Optional<HttpServletResponse> response;
    private HandshakeRequest handshakeRequest;

    private Optional<Subject> subject = Optional.empty();
    private Optional<Map<String, List<Part>>> files = Optional.empty();

    public GraphQLContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response, HandshakeRequest handshakeRequest) {
        this.request = request;
        this.response = response;
        this.handshakeRequest = handshakeRequest;
    }

    public Optional<HttpServletRequest> getRequest() {
        return request;
    }

    public void setRequest(Optional<HttpServletRequest> request) {
        this.request = request;
    }

    public Optional<HttpServletResponse> getResponse() {
        return response;
    }

    public void setResponse(Optional<HttpServletResponse> response) {
        this.response = response;
    }

    public Optional<Subject> getSubject() {
        return subject;
    }

    public void setSubject(Optional<Subject> subject) {
        this.subject = subject;
    }

    public Optional<HandshakeRequest> getHandshakeRequest() {
        return Optional.ofNullable(handshakeRequest);
    }

    public Optional<Map<String, List<Part>>> getFiles() {
        return files;
    }

    public void setFiles(Optional<Map<String, List<Part>>> files) {
        this.files = files;
    }
}
