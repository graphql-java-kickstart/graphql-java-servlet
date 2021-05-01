package graphql.kickstart.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface QueryResponseWriter {

  void write(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
