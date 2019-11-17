package graphql.servlet.context;

import graphql.kickstart.execution.context.GraphQLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;

public interface GraphQLServletContext extends GraphQLContext {

    List<Part> getFileParts();

    Map<String, List<Part>> getParts();

    HttpServletRequest getHttpServletRequest();

    HttpServletResponse getHttpServletResponse();

}
