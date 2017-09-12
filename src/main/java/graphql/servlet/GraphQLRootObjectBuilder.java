package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public interface GraphQLRootObjectBuilder {
    Object build(Optional<HttpServletRequest> req, Optional<HttpServletResponse> resp);
}
