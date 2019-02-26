package graphql.servlet;

import graphql.schema.GraphQLSchema;

/**
 * @author Michiel Oliemans
 */
public abstract class GraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    public static GraphQLHttpServlet with(GraphQLSchema schema) {
        return new ConfiguredGraphQLHttpServlet(GraphQLConfiguration.with(schema).build());
    }

    public static GraphQLHttpServlet with(GraphQLConfiguration configuration) {
        return new ConfiguredGraphQLHttpServlet(configuration);
    }

    @Override
    protected abstract GraphQLConfiguration getConfiguration();

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GraphQLBatchExecutionHandlerFactory getBatchExecutionHandlerFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isAsyncServletMode() {
        throw new UnsupportedOperationException();
    }

}
