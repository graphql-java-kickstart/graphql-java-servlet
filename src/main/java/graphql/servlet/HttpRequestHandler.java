package graphql.servlet;

import java.util.function.BiConsumer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

interface HttpRequestHandler extends BiConsumer<HttpServletRequest, HttpServletResponse> {

  String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
  String APPLICATION_EVENT_STREAM_UTF8 = "text/event-stream;charset=UTF-8";
  String APPLICATION_GRAPHQL = "application/graphql";
  int STATUS_OK = 200;
  int STATUS_BAD_REQUEST = 400;

  @Override
  default void accept(HttpServletRequest request, HttpServletResponse response) {
    try {
      handle(request, response);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void handle(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
