package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class DefaultGraphQLContextBuilder implements GraphQLContextBuilder {

    @Override
    public GraphQLContext build(Optional<HttpServletRequest> req, Optional<HttpServletResponse> resp) {
        return new GraphQLContext(req, resp);
    }

}
