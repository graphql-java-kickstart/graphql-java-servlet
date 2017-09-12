package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public interface GraphQLContextBuilder {
    GraphQLContext build(Optional<HttpServletRequest> req, Optional<HttpServletResponse> resp);
}
