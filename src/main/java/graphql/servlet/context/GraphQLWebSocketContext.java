package graphql.servlet.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GraphQLWebSocketContext extends GraphQLContext {

    List<Part> getFileParts();

    Map<String, List<Part>> getParts();

    Optional<Session> getSession();

    Optional<Object> getConnectResult();

    Optional<HandshakeRequest> getHandshakeRequest();

    Optional<HttpServletRequest> getHttpServletRequest();

    Optional<HttpServletResponse> getHttpServletResponse();
}
