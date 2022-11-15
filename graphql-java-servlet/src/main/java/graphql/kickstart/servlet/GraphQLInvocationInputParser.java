package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

interface GraphQLInvocationInputParser {

  static GraphQLInvocationInputParser create(
      HttpServletRequest request,
      GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper,
      ContextSetting contextSetting)
      throws IOException {
    if ("GET".equalsIgnoreCase(request.getMethod())) {
      return new GraphQLGetInvocationInputParser(
          invocationInputFactory, graphQLObjectMapper, contextSetting);
    }

    try {
      boolean notMultipartRequest =
          request.getContentType() == null
              || !request.getContentType().startsWith("multipart/form-data")
              || request.getParts().isEmpty();
      if (notMultipartRequest) {
        return new GraphQLPostInvocationInputParser(
            invocationInputFactory, graphQLObjectMapper, contextSetting);
      }
      return new GraphQLMultipartInvocationInputParser(
          invocationInputFactory, graphQLObjectMapper, contextSetting);
    } catch (ServletException e) {
      throw new IOException("Cannot get parts of request", e);
    }
  }

  GraphQLInvocationInput getGraphQLInvocationInput(
      HttpServletRequest request, HttpServletResponse response) throws IOException;
}
