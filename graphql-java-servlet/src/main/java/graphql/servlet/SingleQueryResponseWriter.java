package graphql.servlet;

import static graphql.servlet.HttpRequestHandler.APPLICATION_JSON_UTF8;
import static graphql.servlet.HttpRequestHandler.STATUS_OK;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SingleQueryResponseWriter implements QueryResponseWriter {

  private final ExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(APPLICATION_JSON_UTF8);
    response.setStatus(STATUS_OK);
    String responseContent = graphQLObjectMapper.serializeResultAsJson(result);
    response.setContentLength(responseContent.length());
    response.getWriter().write(responseContent);
  }

}
