package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SingleQueryResponseWriter implements QueryResponseWriter {

  private final ExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(HttpRequestHandler.APPLICATION_JSON_UTF8);
    response.setStatus(HttpRequestHandler.STATUS_OK);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    byte[] contentBytes = graphQLObjectMapper.serializeResultAsBytes(result);
    response.setContentLength(contentBytes.length);
    response.getOutputStream().write(contentBytes);
    response.getOutputStream().flush();
  }
}
