package graphql.servlet;

import graphql.schema.GraphQLSchema;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.servlet.input.GraphQLInvocationInputFactory;

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
    protected boolean isAsyncServletMode() {
        throw new UnsupportedOperationException();
    }

}
