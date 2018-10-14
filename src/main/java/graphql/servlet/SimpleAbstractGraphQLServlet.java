package graphql.servlet;

public abstract class SimpleAbstractGraphQLServlet extends AbstractGraphQLHttpServlet {

    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        return null;
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        return null;
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        return null;
    }

}
