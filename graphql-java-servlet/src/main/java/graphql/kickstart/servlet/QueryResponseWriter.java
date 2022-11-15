package graphql.kickstart.servlet;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface QueryResponseWriter {

  void write(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
