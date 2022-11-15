package graphql.kickstart.servlet;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HttpRequestHandler {

  String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
  String APPLICATION_EVENT_STREAM_UTF8 = "text/event-stream;charset=UTF-8";

  int STATUS_OK = 200;
  int STATUS_BAD_REQUEST = 400;
  int STATUS_INTERNAL_SERVER_ERROR = 500;

  void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
