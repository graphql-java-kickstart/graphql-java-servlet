package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;

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
