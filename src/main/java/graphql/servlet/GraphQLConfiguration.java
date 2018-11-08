package graphql.servlet;

import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.List;

public class GraphQLConfiguration {

    private GraphQLInvocationInputFactory invocationInputFactory;
    private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
    private GraphQLObjectMapper objectMapper = GraphQLObjectMapper.newBuilder().build();
    private List<GraphQLServletListener> listeners;
    private boolean asyncServletModeEnabled;

    public static GraphQLConfiguration.Builder with(GraphQLSchema schema) {
        return with(new DefaultGraphQLSchemaProvider(schema));
    }

    public static GraphQLConfiguration.Builder with(GraphQLSchemaProvider schemaProvider) {
        return new Builder(GraphQLInvocationInputFactory.newBuilder(schemaProvider));
    }


    public static class Builder {

        private GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder;
        private GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder().build();
        private GraphQLObjectMapper objectMapper = GraphQLObjectMapper.newBuilder().build();
        private List<GraphQLServletListener> listeners = new ArrayList<>();
        private boolean asyncServletModeEnabled = false;

        private Builder(GraphQLInvocationInputFactory.Builder invocationInputFactoryBuilder) {
            this.invocationInputFactoryBuilder = invocationInputFactoryBuilder;
        }

        public Builder with(GraphQLQueryInvoker queryInvoker) {
            this.queryInvoker = queryInvoker;
            return this;
        }

        public Builder with(GraphQLObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder with(boolean asyncServletModeEnabled) {
            this.asyncServletModeEnabled = asyncServletModeEnabled;
            return this;
        }

        public GraphQLConfiguration build() {
            return null;
        }

    }

}
