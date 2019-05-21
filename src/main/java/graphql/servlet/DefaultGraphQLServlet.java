package graphql.servlet;

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
