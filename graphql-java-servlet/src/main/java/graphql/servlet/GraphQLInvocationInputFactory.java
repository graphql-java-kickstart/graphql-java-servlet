package graphql.servlet;

import graphql.schema.GraphQLSchema;
import graphql.servlet.internal.GraphQLRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Andrew Potter
 */
public class GraphQLInvocationInputFactory {
    private final Supplier<GraphQLSchemaProvider> schemaProviderSupplier;
    private final Supplier<GraphQLContextBuilder> contextBuilderSupplier;
    private final Supplier<GraphQLRootObjectBuilder> rootObjectBuilderSupplier;

    protected GraphQLInvocationInputFactory(Supplier<GraphQLSchemaProvider> schemaProviderSupplier, Supplier<GraphQLContextBuilder> contextBuilderSupplier, Supplier<GraphQLRootObjectBuilder> rootObjectBuilderSupplier) {
        this.schemaProviderSupplier = schemaProviderSupplier;
        this.contextBuilderSupplier = contextBuilderSupplier;
        this.rootObjectBuilderSupplier = rootObjectBuilderSupplier;
    }

    public GraphQLSchemaProvider getSchemaProvider() {
        return schemaProviderSupplier.get();
    }

    public GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest, HttpServletRequest request, HttpServletResponse response) {
        return create(graphQLRequest, request, response, false);
    }

    public GraphQLBatchedInvocationInput create(List<GraphQLRequest> graphQLRequests, HttpServletRequest request, HttpServletResponse response) {
        return create(graphQLRequests, request, response, false);
    }

    public GraphQLSingleInvocationInput createReadOnly(GraphQLRequest graphQLRequest, HttpServletRequest request, HttpServletResponse response) {
        return create(graphQLRequest, request, response, true);
    }

    public GraphQLBatchedInvocationInput createReadOnly(List<GraphQLRequest> graphQLRequests, HttpServletRequest request, HttpServletResponse response) {
        return create(graphQLRequests, request, response, true);
    }

    public GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest) {
        return new GraphQLSingleInvocationInput(
            graphQLRequest,
            schemaProviderSupplier.get().getSchema(),
            contextBuilderSupplier.get().build(),
            rootObjectBuilderSupplier.get().build()
        );
    }

    private GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest, HttpServletRequest request, HttpServletResponse response, boolean readOnly) {
        return new GraphQLSingleInvocationInput(
            graphQLRequest,
            readOnly ? schemaProviderSupplier.get().getReadOnlySchema(request) : schemaProviderSupplier.get().getSchema(request),
            contextBuilderSupplier.get().build(request, response),
            rootObjectBuilderSupplier.get().build(request)
        );
    }

    private GraphQLBatchedInvocationInput create(List<GraphQLRequest> graphQLRequests, HttpServletRequest request, HttpServletResponse response, boolean readOnly) {
        return new GraphQLBatchedInvocationInput(
            graphQLRequests,
            readOnly ? schemaProviderSupplier.get().getReadOnlySchema(request) : schemaProviderSupplier.get().getSchema(request),
            contextBuilderSupplier.get().build(request, response),
            rootObjectBuilderSupplier.get().build(request)
        );
    }

    public GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest, Session session, HandshakeRequest request) {
        return new GraphQLSingleInvocationInput(
            graphQLRequest,
            schemaProviderSupplier.get().getSchema(request),
            contextBuilderSupplier.get().build(session, request),
            rootObjectBuilderSupplier.get().build(request)
        );
    }

    public GraphQLBatchedInvocationInput create(List<GraphQLRequest> graphQLRequest, Session session, HandshakeRequest request) {
        return new GraphQLBatchedInvocationInput(
            graphQLRequest,
            schemaProviderSupplier.get().getSchema(request),
            contextBuilderSupplier.get().build(session, request),
            rootObjectBuilderSupplier.get().build(request)
        );
    }

    public static Builder newBuilder(GraphQLSchema schema) {
        return new Builder(new DefaultGraphQLSchemaProvider(schema));
    }

    public static Builder newBuilder(GraphQLSchemaProvider schemaProvider) {
        return new Builder(schemaProvider);
    }

    public static Builder newBuilder(Supplier<GraphQLSchemaProvider> schemaProviderSupplier) {
        return new Builder(schemaProviderSupplier);
    }

    public static class Builder {
        private final Supplier<GraphQLSchemaProvider> schemaProviderSupplier;
        private Supplier<GraphQLContextBuilder> contextBuilderSupplier = DefaultGraphQLContextBuilder::new;
        private Supplier<GraphQLRootObjectBuilder> rootObjectBuilderSupplier = DefaultGraphQLRootObjectBuilder::new;

        public Builder(GraphQLSchemaProvider schemaProvider) {
            this(() -> schemaProvider);
        }

        public Builder(Supplier<GraphQLSchemaProvider> schemaProviderSupplier) {
            this.schemaProviderSupplier = schemaProviderSupplier;
        }

        public Builder withGraphQLContextBuilder(GraphQLContextBuilder contextBuilder) {
            return withGraphQLContextBuilder(() -> contextBuilder);
        }

        public Builder withGraphQLContextBuilder(Supplier<GraphQLContextBuilder> contextBuilderSupplier) {
            this.contextBuilderSupplier = contextBuilderSupplier;
            return this;
        }

        public Builder withGraphQLRootObjectBuilder(GraphQLRootObjectBuilder rootObjectBuilder) {
            return withGraphQLRootObjectBuilder(() -> rootObjectBuilder);
        }

        public Builder withGraphQLRootObjectBuilder(Supplier<GraphQLRootObjectBuilder> rootObjectBuilderSupplier) {
            this.rootObjectBuilderSupplier = rootObjectBuilderSupplier;
            return this;
        }

        public GraphQLInvocationInputFactory build() {
            return new GraphQLInvocationInputFactory(schemaProviderSupplier, contextBuilderSupplier, rootObjectBuilderSupplier);
        }
    }
}
