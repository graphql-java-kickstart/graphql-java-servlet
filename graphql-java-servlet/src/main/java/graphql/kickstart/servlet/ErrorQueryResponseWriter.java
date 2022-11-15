package graphql.kickstart.servlet;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
