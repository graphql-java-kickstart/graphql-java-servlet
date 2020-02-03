package graphql.kickstart.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ErrorQueryResponseWriter implements QueryResponseWriter {

  private final int statusCode;
  private final String message;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.sendError(statusCode, message);
  }

}
