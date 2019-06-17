package graphql.servlet;

import graphql.servlet.core.AbstractGraphQLHttpServlet;
import graphql.servlet.input.GraphQLInvocationInputFactory;

public class DefaultGraphQLServlet extends AbstractGraphQLHttpServlet {

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        return null;
    }

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        return GraphQLQueryInvoker.newBuilder().build();
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        return GraphQLObjectMapper.newBuilder().build();
    }

    @Override
    protected boolean isAsyncServletMode() {
        return false;
    }

}
