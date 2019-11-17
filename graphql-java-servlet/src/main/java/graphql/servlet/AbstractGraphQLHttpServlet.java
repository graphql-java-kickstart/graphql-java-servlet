package graphql.servlet;

import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.core.GraphQLMBean;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.servlet.core.GraphQLServletListener;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Andrew Potter
 */
@Slf4j
public abstract class AbstractGraphQLHttpServlet extends HttpServlet implements Servlet, GraphQLMBean {

  /**
   * @deprecated use {@link #getConfiguration()} instead
   */
  @Deprecated
  private final List<GraphQLServletListener> listeners;
  private GraphQLConfiguration configuration;
  private HttpRequestHandler requestHandler;

  public AbstractGraphQLHttpServlet() {
    this(null);
  }

  public AbstractGraphQLHttpServlet(List<GraphQLServletListener> listeners) {
    this.listeners = listeners != null ? new ArrayList<>(listeners) : new ArrayList<>();
  }

  /**
   * @deprecated override {@link #getConfiguration()} instead
   */
  @Deprecated
  protected abstract GraphQLQueryInvoker getQueryInvoker();

  /**
   * @deprecated override {@link #getConfiguration()} instead
   */
  @Deprecated
  protected abstract GraphQLInvocationInputFactory getInvocationInputFactory();

  /**
   * @deprecated override {@link #getConfiguration()} instead
   */
  @Deprecated
  protected abstract GraphQLObjectMapper getGraphQLObjectMapper();

  /**
   * @deprecated override {@link #getConfiguration()} instead
   */
  @Deprecated
  protected abstract boolean isAsyncServletMode();

  protected GraphQLConfiguration getConfiguration() {
    return GraphQLConfiguration.with(getInvocationInputFactory())
        .with(getQueryInvoker())
        .with(getGraphQLObjectMapper())
        .with(isAsyncServletMode())
        .with(listeners)
        .build();
  }

  @Override
  public void init() {
    if (configuration == null) {
      this.configuration = getConfiguration();
      this.requestHandler = new HttpRequestHandlerImpl(configuration);
    }
  }

  public void addListener(GraphQLServletListener servletListener) {
    if (configuration != null) {
      configuration.add(servletListener);
    } else {
      listeners.add(servletListener);
    }
  }

  public void removeListener(GraphQLServletListener servletListener) {
    if (configuration != null) {
      configuration.remove(servletListener);
    } else {
      listeners.remove(servletListener);
    }
  }

  @Override
  public String[] getQueries() {
    return configuration.getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType()
        .getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
  }

  @Override
  public String[] getMutations() {
    return configuration.getInvocationInputFactory().getSchemaProvider().getSchema().getMutationType()
        .getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
  }

  @Override
  public String executeQuery(String query) {
    try {
      return configuration.getObjectMapper().serializeResultAsJson(configuration.getQueryInvoker()
          .query(configuration.getInvocationInputFactory().create(new GraphQLRequest(query, new HashMap<>(), null))));
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private void doRequestAsync(HttpServletRequest request, HttpServletResponse response, HttpRequestHandler handler) {
    if (configuration.isAsyncServletModeEnabled()) {
      AsyncContext asyncContext = request.startAsync(request, response);
      HttpServletRequest asyncRequest = (HttpServletRequest) asyncContext.getRequest();
      HttpServletResponse asyncResponse = (HttpServletResponse) asyncContext.getResponse();
      configuration.getAsyncExecutor().execute(() -> doRequest(asyncRequest, asyncResponse, handler, asyncContext));
    } else {
      doRequest(request, response, handler, null);
    }
  }

  private void doRequest(HttpServletRequest request, HttpServletResponse response, HttpRequestHandler handler,
      AsyncContext asyncContext) {

    List<GraphQLServletListener.RequestCallback> requestCallbacks = runListeners(l -> l.onRequest(request, response));

    try {
      handler.handle(request, response);
      runCallbacks(requestCallbacks, c -> c.onSuccess(request, response));
    } catch (Throwable t) {
      log.error("Error executing GraphQL request!", t);
      runCallbacks(requestCallbacks, c -> c.onError(request, response, t));
    } finally {
      runCallbacks(requestCallbacks, c -> c.onFinally(request, response));
      if (asyncContext != null) {
        asyncContext.complete();
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    init();
    doRequestAsync(req, resp, requestHandler);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    init();
    doRequestAsync(req, resp, requestHandler);
  }

  private <R> List<R> runListeners(Function<? super GraphQLServletListener, R> action) {
    return configuration.getListeners().stream()
        .map(listener -> {
          try {
            return action.apply(listener);
          } catch (Throwable t) {
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
      } catch (Throwable t) {
        log.error("Error running callback: {}", callback, t);
      }
    });
  }

}
