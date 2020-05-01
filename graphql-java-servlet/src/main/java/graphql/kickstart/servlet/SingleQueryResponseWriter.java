package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.cache.CachedResponse;
import graphql.kickstart.servlet.cache.GraphQLResponseCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
class SingleQueryResponseWriter implements QueryResponseWriter {

  private final ExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final GraphQLInvocationInput invocationInput;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response, GraphQLResponseCache responseCache) throws IOException {
    response.setContentType(HttpRequestHandler.APPLICATION_JSON_UTF8);
    response.setStatus(HttpRequestHandler.STATUS_OK);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String responseContent = graphQLObjectMapper.serializeResultAsJson(result);
    byte[] contentBytes = responseContent.getBytes(StandardCharsets.UTF_8);

    if (responseCache != null && responseCache.isCacheable(request, invocationInput)) {
      try {
        responseCache.put(request, invocationInput, CachedResponse.ofContent(contentBytes));
      } catch (Throwable t) {
        log.warn(t.getMessage(), t);
        log.warn("Ignore read from cache, unexpected error happened");
      }
    }

    response.setContentLength(contentBytes.length);
    response.getOutputStream().write(contentBytes);
  }

}
