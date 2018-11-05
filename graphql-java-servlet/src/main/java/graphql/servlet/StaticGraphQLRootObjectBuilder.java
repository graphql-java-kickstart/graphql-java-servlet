package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public class StaticGraphQLRootObjectBuilder implements GraphQLRootObjectBuilder {

    private final Object rootObject;

    public StaticGraphQLRootObjectBuilder(Object rootObject) {
        this.rootObject = rootObject;
    }

    @Override
    public Object build(HttpServletRequest req) {
        return rootObject;
    }

    @Override
    public Object build(HandshakeRequest req) {
        return rootObject;
    }

    @Override
    public Object build() {
        return rootObject;
    }
}
