package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class StaticGraphQLRootObjectBuilder implements GraphQLRootObjectBuilder {

    private final Object rootObject;

    public StaticGraphQLRootObjectBuilder(Object rootObject) {
        this.rootObject = rootObject;
    }

    @Override
    public Object build(Optional<HttpServletRequest> req, Optional<HttpServletResponse> resp) {
        return rootObject;
    }
}
