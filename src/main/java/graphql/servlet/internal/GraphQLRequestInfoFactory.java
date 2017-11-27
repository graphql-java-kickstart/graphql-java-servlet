package graphql.servlet.internal;

import graphql.servlet.GraphQLContextBuilder;
import graphql.servlet.GraphQLRootObjectBuilder;
import graphql.servlet.GraphQLSchemaProvider;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;
import java.util.function.Supplier;

/**
 * @author Andrew Potter
 */
public class GraphQLRequestInfoFactory {
    private final Supplier<GraphQLSchemaProvider> schemaProvider;
    private final Supplier<GraphQLContextBuilder> contextBuilder;
    private final Supplier<GraphQLRootObjectBuilder> rootObjectBuilder;

    public GraphQLRequestInfoFactory(Supplier<GraphQLSchemaProvider> schemaProvider, Supplier<GraphQLContextBuilder> contextBuilder, Supplier<GraphQLRootObjectBuilder> rootObjectBuilder) {
        this.schemaProvider = schemaProvider;
        this.contextBuilder = contextBuilder;
        this.rootObjectBuilder = rootObjectBuilder;
    }

    public GraphQLRequestInfo create(HttpServletRequest request) {
        return create(request, false);
    }

    public GraphQLRequestInfo createReadOnly(HttpServletRequest request) {
        return create(request, true);
    }

    private GraphQLRequestInfo create(HttpServletRequest request, boolean readOnly) {
        return new GraphQLRequestInfo(
            readOnly ? schemaProvider.get().getReadOnlySchema(request) : schemaProvider.get().getSchema(request),
            contextBuilder.get().build(request),
            rootObjectBuilder.get().build(request)
        );
    }

    public GraphQLRequestInfo create(HandshakeRequest request) {
        return new GraphQLRequestInfo(
            schemaProvider.get().getSchema(request),
            contextBuilder.get().build(request),
            rootObjectBuilder.get().build(request)
        );
    }
}
