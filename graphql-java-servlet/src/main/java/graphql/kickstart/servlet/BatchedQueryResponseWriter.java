package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class BatchedQueryResponseWriter implements QueryResponseWriter {
  private final List<ExecutionResult> results;
  private final GraphQLObjectMapper graphQLObjectMapper;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(HttpRequestHandler.APPLICATION_JSON_UTF8);
    response.setStatus(HttpRequestHandler.STATUS_OK);

    // Use direct serialization to byte arrays and avoid any string concatenation to save multiple
    // GiB of memory allocation during large response processing.
    List<byte[]> serializedResults = new ArrayList<>(2 * results.size() + 1);

    if (!results.isEmpty()) {
      serializedResults.add("[".getBytes(StandardCharsets.UTF_8));
    } else {
      serializedResults.add("[]".getBytes(StandardCharsets.UTF_8));
    }
    long totalLength = serializedResults.get(0).length;

    // '[', ',' and ']' are all 1 byte in UTF-8.
    for (int i = 0; i < results.size(); i++) {
      byte[] currentResult = graphQLObjectMapper.serializeResultAsBytes(results.get(i));
      serializedResults.add(currentResult);

      if (i != results.size() - 1) {
        serializedResults.add(",".getBytes(StandardCharsets.UTF_8));
      } else {
        serializedResults.add("]".getBytes(StandardCharsets.UTF_8));
      }
      totalLength += currentResult.length + 1; // result.length + ',' or ']'
    }

    if (totalLength > Integer.MAX_VALUE) {
      throw new IllegalStateException(
          "Response size exceed 2GiB. Query will fail. Seen size: " + totalLength);
    }
    response.setContentLength((int) totalLength);

    for (byte[] result : serializedResults) {
      response.getOutputStream().write(result);
    }
  }
}
