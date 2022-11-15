package graphql.kickstart.servlet.context;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/** @deprecated Use {@link graphql.kickstart.execution.context.GraphQLKickstartContext} instead */
public interface GraphQLServletContext extends GraphQLKickstartContext {

  List<Part> getFileParts();

  Map<String, List<Part>> getParts();

  HttpServletRequest getHttpServletRequest();

  HttpServletResponse getHttpServletResponse();
}
