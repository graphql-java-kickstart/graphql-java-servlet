package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.cache.CachedResponse;
import graphql.kickstart.servlet.cache.GraphQLResponseCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
class BatchedQueryResponseWriter implements QueryResponseWriter {

  private final List<ExecutionResult> results;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final GraphQLInvocationInput invocationInput;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response, GraphQLResponseCache responseCache) throws IOException {
    response.setContentType(HttpRequestHandler.APPLICATION_JSON_UTF8);
    response.setStatus(HttpRequestHandler.STATUS_OK);

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
    byte[] contentBytes = responseContent.getBytes(StandardCharsets.UTF_8);

    if (responseCache != null) {
      try {
        responseCache.cacheResponse(invocationInput, CachedResponse.ofContent(contentBytes));
      } catch (Throwable t) {
        log.warn(t.getMessage(), t);
        log.warn("Ignore read from cache, unexpected error happened");
      }
    }

    response.setContentLength(contentBytes.length);
    response.getOutputStream().write(contentBytes);
  }

}
