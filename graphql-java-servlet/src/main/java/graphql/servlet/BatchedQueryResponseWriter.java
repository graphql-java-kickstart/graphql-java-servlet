package graphql.servlet;

import static graphql.servlet.HttpRequestHandler.APPLICATION_JSON_UTF8;
import static graphql.servlet.HttpRequestHandler.STATUS_OK;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class BatchedQueryResponseWriter implements QueryResponseWriter {

  private final List<ExecutionResult> results;
  private final GraphQLObjectMapper graphQLObjectMapper;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType(APPLICATION_JSON_UTF8);
    response.setStatus(STATUS_OK);

    Iterator<ExecutionResult> executionInputIterator = results.iterator();
    StringBuilder responseBuilder = new StringBuilder();
    responseBuilder.append('[');
    while (executionInputIterator.hasNext()) {
      responseBuilder.append(graphQLObjectMapper.serializeResultAsJson(executionInputIterator.next()));
      if (executionInputIterator.hasNext()) {
        responseBuilder.append(',');
      }
    }
    responseBuilder.append(']');

    String responseContent = responseBuilder.toString();
    response.setContentLength(responseContent.length());
    response.getWriter().write(responseContent);
  }

}
