package graphql.servlet.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GraphQLServletContext extends GraphQLContext {

    List<Part> getFileParts();

    Map<String, List<Part>> getParts();

    HttpServletRequest getHttpServletRequest();

    HttpServletResponse getHttpServletResponse();

}
