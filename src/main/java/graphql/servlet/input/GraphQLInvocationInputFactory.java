package graphql.servlet.input;

import graphql.schema.GraphQLSchema;
import graphql.servlet.context.DefaultGraphQLContextBuilder;
import graphql.servlet.core.DefaultGraphQLRootObjectBuilder;
import graphql.servlet.config.DefaultGraphQLSchemaProvider;
import graphql.servlet.context.ContextSetting;
import graphql.servlet.context.GraphQLContextBuilder;
import graphql.servlet.core.GraphQLRootObjectBuilder;
import graphql.servlet.config.GraphQLSchemaProvider;
import graphql.servlet.core.internal.GraphQLRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.Map;
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
        return create(graphQLRequest, request, response, null);
    }

    public GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest, HttpServletRequest request, HttpServletResponse response, Map<String,
        List<Part>> fileParts) {
        return create(graphQLRequest, request, response, false, fileParts);
    }

    public GraphQLBatchedInvocationInput create(ContextSetting contextSetting, List<GraphQLRequest> graphQLRequests, HttpServletRequest request,
                                                HttpServletResponse response) {
        return create(contextSetting, graphQLRequests, request, response, null);
    }

    public GraphQLBatchedInvocationInput create(ContextSetting contextSetting, List<GraphQLRequest> graphQLRequests, HttpServletRequest request,
                                                HttpServletResponse response, Map<String, List<Part>> fileParts) {
        return create(contextSetting, graphQLRequests, request, response, false, fileParts);
    }

    public GraphQLSingleInvocationInput createReadOnly(GraphQLRequest graphQLRequest, HttpServletRequest request, HttpServletResponse response) {
        return create(graphQLRequest, request, response, true, null);
    }

    public GraphQLBatchedInvocationInput createReadOnly(ContextSetting contextSetting, List<GraphQLRequest> graphQLRequests, HttpServletRequest request, HttpServletResponse response) {
        return create(contextSetting, graphQLRequests, request, response, true, null);
    }

    public GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest) {
        return new GraphQLSingleInvocationInput(
            graphQLRequest,
            schemaProviderSupplier.get().getSchema(),
            contextBuilderSupplier.get().build(),
            rootObjectBuilderSupplier.get().build()
        );
    }

    private GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest, HttpServletRequest request, HttpServletResponse response,
                                                boolean readOnly, Map<String, List<Part>> fileParts) {
        return new GraphQLSingleInvocationInput(
            graphQLRequest,
            readOnly ? schemaProviderSupplier.get().getReadOnlySchema(request) : schemaProviderSupplier.get().getSchema(request),
            contextBuilderSupplier.get().build(request, response,fileParts),
            rootObjectBuilderSupplier.get().build(request)
        );
    }

    private GraphQLBatchedInvocationInput create(ContextSetting contextSetting, List<GraphQLRequest> graphQLRequests, HttpServletRequest request,
                                                 HttpServletResponse response, boolean readOnly, Map<String, List<Part>> fileParts) {
        return contextSetting.getBatch(
            graphQLRequests,
            readOnly ? schemaProviderSupplier.get().getReadOnlySchema(request) : schemaProviderSupplier.get().getSchema(request),
            () -> contextBuilderSupplier.get().build(request, response, fileParts),
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

    public GraphQLBatchedInvocationInput create(ContextSetting contextSetting, List<GraphQLRequest> graphQLRequest, Session session, HandshakeRequest request) {
        return contextSetting.getBatch(
            graphQLRequest,
            schemaProviderSupplier.get().getSchema(request),
            () -> contextBuilderSupplier.get().build(session, request),
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
