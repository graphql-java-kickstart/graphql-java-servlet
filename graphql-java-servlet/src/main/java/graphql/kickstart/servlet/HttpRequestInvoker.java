package graphql.kickstart.servlet;

import graphql.kickstart.execution.input.GraphQLInvocationInput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HttpRequestInvoker {

  void execute(
      GraphQLInvocationInput invocationInput,
      HttpServletRequest request,
      HttpServletResponse response,
      ListenerHandler listenerHandler);
}
