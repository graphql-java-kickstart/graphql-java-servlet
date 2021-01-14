package graphql.kickstart.servlet;

import static graphql.kickstart.execution.GraphQLRequest.createQueryOnlyRequest;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.servlet.core.GraphQLMBean;
import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.schema.GraphQLFieldDefinition;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Andrew Potter
 */
@Slf4j
public abstract class AbstractGraphQLHttpServlet extends HttpServlet implements Servlet,
    GraphQLMBean {

  protected abstract GraphQLConfiguration getConfiguration();

  public void addListener(GraphQLServletListener servletListener) {
    getConfiguration().add(servletListener);
  }

  public void removeListener(GraphQLServletListener servletListener) {
    getConfiguration().remove(servletListener);
  }

  @Override
  public String[] getQueries() {
    return getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema()
        .getQueryType()
        .getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
  }

  @Override
  public String[] getMutations() {
    return getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema()
        .getMutationType()
        .getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
  }

  @Override
  public String executeQuery(String query) {
    try {
      GraphQLRequest graphQLRequest = createQueryOnlyRequest(query);
      GraphQLSingleInvocationInput invocationInput = getConfiguration().getInvocationInputFactory()
          .create(graphQLRequest);
      ExecutionResult result = getConfiguration().getGraphQLInvoker().query(invocationInput)
          .getResult();
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
    List<GraphQLServletListener.RequestCallback> requestCallbacks = runListeners(
        l -> l.onRequest(request, response));

    try {
      getConfiguration().getHttpRequestHandler().handle(request, response);
      runCallbacks(requestCallbacks, c -> c.onSuccess(request, response));
    } catch (Exception t) {
      log.error("Error executing GraphQL request!", t);
      runCallbacks(requestCallbacks, c -> c.onError(request, response, t));
    } finally {
      runCallbacks(requestCallbacks, c -> c.onFinally(request, response));
    }
  }

  private <R> List<R> runListeners(Function<? super GraphQLServletListener, R> action) {
    return getConfiguration().getListeners().stream()
        .map(listener -> {
          try {
            return action.apply(listener);
          } catch (Exception t) {
            log.error("Error running listener: {}", listener, t);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private <T> void runCallbacks(List<T> callbacks, Consumer<T> action) {
    callbacks.forEach(callback -> {
      try {
        action.accept(callback);
      } catch (Exception t) {
        log.error("Error running callback: {}", callback, t);
      }
    });
  }

}
