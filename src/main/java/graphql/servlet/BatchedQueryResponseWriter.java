package graphql.servlet;

import static graphql.servlet.HttpRequestHandler.APPLICATION_JSON_UTF8;
import static graphql.servlet.HttpRequestHandler.STATUS_OK;

import graphql.ExecutionResult;
import graphql.servlet.core.GraphQLObjectMapper;
import java.io.IOException;
import java.io.Writer;
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

    Writer writer = response.getWriter();
    Iterator<ExecutionResult> executionInputIterator = results.iterator();
    writer.write("[");
    while (executionInputIterator.hasNext()) {
      String result = graphQLObjectMapper.serializeResultAsJson(executionInputIterator.next());
      writer.write(result);
      if (executionInputIterator.hasNext()) {
        writer.write(",");
      }
    }
    writer.write("]");
  }

}
