package graphql.servlet;

import graphql.schema.GraphQLSchema;

import java.util.List;

/**
 * @author Andrew Potter
 */
public class SimpleGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLObjectMapper graphQLObjectMapper;

    protected SimpleGraphQLHttpServlet(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper, List<GraphQLServletListener> listeners, boolean asyncServletMode) {
        super(listeners, asyncServletMode);
        this.invocationInputFactory = invocationInputFactory;
        this.queryInvoker = queryInvoker;
        this.graphQLObjectMapper = graphQLObjectMapper;
    }

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        return queryInvoker;
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        return invocationInputFactory;
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        return graphQLObjectMapper;
    }

    public static Builder newBuilder(GraphQLSchema schema) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schema).build());
    }

    public static Builder newBuilder(GraphQLSchemaProvider schemaProvider) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schemaProvider).build());
    }

    public static Builder newBuilder(GraphQLInvocationInputFactory invocationInputFactory) {
        return new Builder(invocationInputFactory);
    }

    public static class Builder {
        private final GraphQLInvocationInputFactory invocationInputFactory;
        private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
        private GraphQLObjectMapper graphQLObjectMapper = GraphQLObjectMapper.newBuilder().build();
        private List<GraphQLServletListener> listeners;
        private boolean asyncServletMode;

        Builder(GraphQLInvocationInputFactory invocationInputFactory) {
            this.invocationInputFactory = invocationInputFactory;
        }

        public Builder withQueryInvoker(GraphQLQueryInvoker queryInvoker) {
            this.queryInvoker = queryInvoker;
            return this;
        }

        public Builder withObjectMapper(GraphQLObjectMapper objectMapper) {
            this.graphQLObjectMapper = objectMapper;
            return this;
        }

        public Builder withAsyncServletMode(boolean asyncServletMode) {
            this.asyncServletMode = asyncServletMode;
            return this;
        }

        public Builder withListeners(List<GraphQLServletListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public SimpleGraphQLHttpServlet build() {
            return new SimpleGraphQLHttpServlet(invocationInputFactory, queryInvoker, graphQLObjectMapper, listeners, asyncServletMode);
        }
    }
}
