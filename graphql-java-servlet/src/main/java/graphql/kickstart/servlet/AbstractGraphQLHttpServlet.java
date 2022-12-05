package graphql.kickstart.servlet;

import static graphql.kickstart.execution.GraphQLRequest.createQueryOnlyRequest;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.servlet.core.GraphQLMBean;
import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.schema.GraphQLFieldDefinition;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/** @author Andrew Potter */
@Slf4j
public abstract class AbstractGraphQLHttpServlet extends HttpServlet
    implements Servlet, GraphQLMBean {

  protected abstract GraphQLConfiguration getConfiguration();

  public void addListener(GraphQLServletListener servletListener) {
    getConfiguration().add(servletListener);
  }

  public void removeListener(GraphQLServletListener servletListener) {
    getConfiguration().remove(servletListener);
  }

  @Override
  public String[] getQueries() {
    return getConfiguration()
        .getInvocationInputFactory()
        .getSchemaProvider()
        .getSchema()
        .getQueryType()
        .getFieldDefinitions()
        .stream()
        .map(GraphQLFieldDefinition::getName)
        .toArray(String[]::new);
  }

  @Override
  public String[] getMutations() {
    return getConfiguration()
        .getInvocationInputFactory()
        .getSchemaProvider()
        .getSchema()
        .getMutationType()
        .getFieldDefinitions()
        .stream()
        .map(GraphQLFieldDefinition::getName)
        .toArray(String[]::new);
  }

  @Override
  public String executeQuery(String query) {
    try {
      GraphQLRequest graphQLRequest = createQueryOnlyRequest(query);
      GraphQLSingleInvocationInput invocationInput =
          getConfiguration().getInvocationInputFactory().create(graphQLRequest);
      ExecutionResult result =
          getConfiguration().getGraphQLInvoker().query(invocationInput).getResult();
      return getConfiguration().getObjectMapper().serializeResultAsJson(result);
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    doRequest(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    doRequest(req, resp);
  }

  private void doRequest(HttpServletRequest request, HttpServletResponse response) {
    try {
      getConfiguration().getHttpRequestHandler().handle(request, response);
    } catch (Exception t) {
      log.error("Error executing GraphQL request!", t);
    }
  }
}
