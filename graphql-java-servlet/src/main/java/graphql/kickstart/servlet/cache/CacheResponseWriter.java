package graphql.kickstart.servlet.cache;

import graphql.kickstart.servlet.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CacheResponseWriter {

  public void write(HttpServletRequest request, HttpServletResponse response, CachedResponse cachedResponse)
      throws IOException {
    if (cachedResponse.isError()) {
      response.sendError(cachedResponse.getErrorStatusCode(), cachedResponse.getErrorMessage());
    } else {
      response.setContentType(HttpRequestHandler.APPLICATION_JSON_UTF8);
      response.setStatus(HttpRequestHandler.STATUS_OK);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.setContentLength(cachedResponse.getContentBytes().length);
      response.getOutputStream().write(cachedResponse.getContentBytes());
    }
  }

}
