package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
class SingleQueryResponseWriter implements QueryResponseWriter {

  private final ExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(APPLICATION_JSON_UTF8);
    response.setStatus(STATUS_OK);
    response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
    String responseContent = graphQLObjectMapper.serializeResultAsJson(result);
    response.setContentLength(responseContent.getBytes(StandardCharsets.UTF_8).length);
    response.getWriter().write(responseContent);
  }

}
