package graphql.servlet;

import static graphql.servlet.HttpRequestHandler.APPLICATION_GRAPHQL;
import static graphql.servlet.HttpRequestHandler.STATUS_BAD_REQUEST;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.reactive.SingleSubscriberPublisher;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLFieldDefinition;
import graphql.servlet.config.GraphQLConfiguration;
import graphql.servlet.context.ContextSetting;
import graphql.servlet.core.GraphQLMBean;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.core.GraphQLQueryInvoker;
import graphql.servlet.core.GraphQLServletListener;
import graphql.servlet.core.internal.GraphQLRequest;
import graphql.servlet.core.internal.VariableMapper;
import graphql.servlet.input.BatchInputPreProcessResult;
import graphql.servlet.input.BatchInputPreProcessor;
import graphql.servlet.input.GraphQLBatchedInvocationInput;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import graphql.servlet.input.GraphQLSingleInvocationInput;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Andrew Potter
 */
@Slf4j
public abstract class AbstractGraphQLHttpServlet extends HttpServlet implements Servlet, GraphQLMBean {

  private static final String[] MULTIPART_KEYS = new String[]{"operations", "graphql", "query"};
  /**
   * @deprecated use {@link #getConfiguration()} instead
   */
  @Deprecated
  private final List<GraphQLServletListener> listeners;
  private GraphQLConfiguration configuration;
  private HttpRequestHandler getHandler;
  private HttpRequestHandler postHandler;

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
    if (configuration != null) {
      return;
    }
    this.configuration = getConfiguration();
    this.getHandler = new HttpGetRequestHandler(configuration);

    this.postHandler = (request, response) -> {
      GraphQLInvocationInputFactory invocationInputFactory = configuration.getInvocationInputFactory();
      GraphQLObjectMapper graphQLObjectMapper = configuration.getObjectMapper();
      GraphQLQueryInvoker queryInvoker = configuration.getQueryInvoker();

      try {
        if (APPLICATION_GRAPHQL.equals(request.getContentType())) {
          String query = CharStreams.toString(request.getReader());
          query(queryInvoker, graphQLObjectMapper,
              invocationInputFactory.create(new GraphQLRequest(query, null, null), request, response),
              request, response);
        } else if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")
            && !request.getParts().isEmpty()) {
          final Map<String, List<Part>> fileItems = request.getParts()
              .stream()
              .collect(Collectors.groupingBy(Part::getName));

          for (String key : MULTIPART_KEYS) {
            // Check to see if there is a part under the key we seek
            if (!fileItems.containsKey(key)) {
              continue;
            }

            final Optional<Part> queryItem = getFileItem(fileItems, key);
            if (!queryItem.isPresent()) {
              // If there is a part, but we don't see an item, then break and return BAD_REQUEST
              break;
            }

            InputStream inputStream = asMarkableInputStream(queryItem.get().getInputStream());

            final Optional<Map<String, List<String>>> variablesMap =
                getFileItem(fileItems, "map").map(graphQLObjectMapper::deserializeMultipartMap);

            if (isBatchedQuery(inputStream)) {
              List<GraphQLRequest> graphQLRequests =
                  graphQLObjectMapper.readBatchedGraphQLRequest(inputStream);
              variablesMap.ifPresent(map -> graphQLRequests.forEach(r -> mapMultipartVariables(r, map, fileItems)));
              GraphQLBatchedInvocationInput batchedInvocationInput = invocationInputFactory
                  .create(configuration.getContextSetting(),
                      graphQLRequests, request, response);
              queryBatched(queryInvoker, batchedInvocationInput, request, response, configuration);
              return;
            } else {
              GraphQLRequest graphQLRequest;
              if ("query".equals(key)) {
                graphQLRequest = buildRequestFromQuery(inputStream, graphQLObjectMapper, fileItems);
              } else {
                graphQLRequest = graphQLObjectMapper.readGraphQLRequest(inputStream);
              }

              variablesMap.ifPresent(m -> mapMultipartVariables(graphQLRequest, m, fileItems));
              GraphQLSingleInvocationInput invocationInput =
                  invocationInputFactory.create(graphQLRequest, request, response);
              query(queryInvoker, graphQLObjectMapper, invocationInput, request, response);
              return;
            }
          }

          response.setStatus(STATUS_BAD_REQUEST);
          log.info("Bad POST multipart request: no part named " + Arrays.toString(MULTIPART_KEYS));
        } else {
          // this is not a multipart request
          InputStream inputStream = asMarkableInputStream(request.getInputStream());

          if (isBatchedQuery(inputStream)) {
            List<GraphQLRequest> requests = graphQLObjectMapper.readBatchedGraphQLRequest(inputStream);
            GraphQLBatchedInvocationInput batchedInvocationInput =
                invocationInputFactory.create(configuration.getContextSetting(), requests, request, response);
            queryBatched(queryInvoker, batchedInvocationInput, request, response, configuration);
          } else {
            query(queryInvoker, graphQLObjectMapper,
                invocationInputFactory.create(graphQLObjectMapper.readGraphQLRequest(inputStream), request, response),
                request, response);
          }
        }
      } catch (Exception e) {
        log.info("Bad POST request: parsing failed", e);
        response.setStatus(STATUS_BAD_REQUEST);
      }
    };
  }

  private InputStream asMarkableInputStream(InputStream inputStream) {
    if (!inputStream.markSupported()) {
      return new BufferedInputStream(inputStream);
    }
    return inputStream;
  }

  private GraphQLRequest buildRequestFromQuery(InputStream inputStream,
      GraphQLObjectMapper graphQLObjectMapper,
      Map<String, List<Part>> fileItems) throws IOException {
    GraphQLRequest graphQLRequest;
    String query = new String(ByteStreams.toByteArray(inputStream));

    Map<String, Object> variables = null;
    final Optional<Part> variablesItem = getFileItem(fileItems, "variables");
    if (variablesItem.isPresent()) {
      variables = graphQLObjectMapper
          .deserializeVariables(new String(ByteStreams.toByteArray(variablesItem.get().getInputStream())));
    }

    String operationName = null;
    final Optional<Part> operationNameItem = getFileItem(fileItems, "operationName");
    if (operationNameItem.isPresent()) {
      operationName = new String(ByteStreams.toByteArray(operationNameItem.get().getInputStream())).trim();
    }

    graphQLRequest = new GraphQLRequest(query, variables, operationName);
    return graphQLRequest;
  }

  private void mapMultipartVariables(GraphQLRequest request,
      Map<String, List<String>> variablesMap,
      Map<String, List<Part>> fileItems) {
    Map<String, Object> variables = request.getVariables();

    variablesMap.forEach((partName, objectPaths) -> {
      Part part = getFileItem(fileItems, partName)
          .orElseThrow(() -> new RuntimeException("unable to find part name " +
              partName +
              " as referenced in the variables map"));

      objectPaths.forEach(objectPath -> VariableMapper.mapVariable(objectPath, variables, part));
    });
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
      response.setStatus(500);
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
    doRequestAsync(req, resp, getHandler);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    init();
    doRequestAsync(req, resp, postHandler);
  }

  private Optional<Part> getFileItem(Map<String, List<Part>> fileItems, String name) {
    return Optional.ofNullable(fileItems.get(name)).filter(list -> !list.isEmpty()).map(list -> list.get(0));
  }

  private void query(GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper,
      GraphQLSingleInvocationInput invocationInput,
      HttpServletRequest req, HttpServletResponse resp) throws IOException {
    ExecutionResult result = queryInvoker.query(invocationInput);

    boolean isDeferred =
        Objects.nonNull(result.getExtensions()) && result.getExtensions().containsKey(GraphQL.DEFERRED_RESULTS);

    if (!(result.getData() instanceof Publisher || isDeferred)) {
      resp.setContentType(APPLICATION_JSON_UTF8);
      resp.setStatus(STATUS_OK);
      graphQLObjectMapper.serializeResultAsJson(resp.getWriter(), result);
    } else {
      if (req == null) {
        throw new IllegalStateException("Http servlet request can not be null");
      }
      resp.setContentType(APPLICATION_EVENT_STREAM_UTF8);
      resp.setStatus(STATUS_OK);

      boolean isInAsyncThread = req.isAsyncStarted();
      AsyncContext asyncContext = isInAsyncThread ? req.getAsyncContext() : req.startAsync(req, resp);
      asyncContext.setTimeout(configuration.getSubscriptionTimeout());
      AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
      asyncContext.addListener(new SubscriptionAsyncListener(subscriptionRef));
      ExecutionResultSubscriber subscriber = new ExecutionResultSubscriber(subscriptionRef, asyncContext,
          graphQLObjectMapper);
      List<Publisher<ExecutionResult>> publishers = new ArrayList<>();
      if (result.getData() instanceof Publisher) {
        publishers.add(result.getData());
      } else {
        publishers.add(new StaticDataPublisher<>(result));
        final Publisher<ExecutionResult> deferredResultsPublisher = (Publisher<ExecutionResult>) result.getExtensions()
            .get(GraphQL.DEFERRED_RESULTS);
        publishers.add(deferredResultsPublisher);
      }
      publishers.forEach(it -> it.subscribe(subscriber));

      if (isInAsyncThread) {
        // We need to delay the completion of async context until after the subscription has terminated, otherwise the AsyncContext is prematurely closed.
        try {
          subscriber.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private void queryBatched(GraphQLQueryInvoker queryInvoker, GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response, GraphQLConfiguration configuration) throws IOException {
    BatchInputPreProcessor batchInputPreProcessor = configuration.getBatchInputPreProcessor();
    ContextSetting contextSetting = configuration.getContextSetting();
    BatchInputPreProcessResult batchInputPreProcessResult = batchInputPreProcessor
        .preProcessBatch(batchedInvocationInput, request, response);
    if (batchInputPreProcessResult.isExecutable()) {
      List<ExecutionResult> results = queryInvoker
          .query(batchInputPreProcessResult.getBatchedInvocationInput().getExecutionInputs(),
              contextSetting);
      response.setContentType(AbstractGraphQLHttpServlet.APPLICATION_JSON_UTF8);
      response.setStatus(AbstractGraphQLHttpServlet.STATUS_OK);
      Writer writer = response.getWriter();
      Iterator<ExecutionResult> executionInputIterator = results.iterator();
      writer.write("[");
      GraphQLObjectMapper graphQLObjectMapper = configuration.getObjectMapper();
      while (executionInputIterator.hasNext()) {
        String result = graphQLObjectMapper.serializeResultAsJson(executionInputIterator.next());
        writer.write(result);
        if (executionInputIterator.hasNext()) {
          writer.write(",");
        }
      }
      writer.write("]");
    } else {
      response.sendError(batchInputPreProcessResult.getStatusCode(), batchInputPreProcessResult.getStatusMessage());
    }
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

  private boolean isBatchedQuery(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      return false;
    }

    final int BUFFER_SIZE = 128;
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[BUFFER_SIZE];
    int length;

    inputStream.mark(BUFFER_SIZE);
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
      if (isArrayStart(result.toString())) {
        inputStream.reset();
        return true;
      }
    }

    inputStream.reset();
    return false;
  }

  private boolean isBatchedQuery(String query) {
    return isArrayStart(query);
  }

  private boolean isArrayStart(String s) {
    return s != null && s.trim().startsWith("[");
  }

}
