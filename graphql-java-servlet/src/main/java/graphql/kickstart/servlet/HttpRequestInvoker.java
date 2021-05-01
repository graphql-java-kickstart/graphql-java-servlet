package graphql.kickstart.servlet;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpRequestInvoker {

  void execute(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response);
}
