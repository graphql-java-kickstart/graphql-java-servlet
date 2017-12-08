package graphql.servlet;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.servlet.internal.GraphQLRequest;
import graphql.servlet.internal.GraphQLRequestInfo;
import graphql.servlet.internal.GraphQLRequestInfoFactory;
import graphql.servlet.internal.VariablesDeserializer;
import graphql.servlet.internal.WsSessionSubscriptions;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Andrew Potter
 */
public abstract class GraphQLServlet extends HttpServlet implements Servlet, GraphQLMBean {

    public static final Logger log = LoggerFactory.getLogger(GraphQLServlet.class);

    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final int STATUS_OK = 200;
    public static final int STATUS_BAD_REQUEST = 400;

    private static final GraphQLRequest INTROSPECTION_REQUEST = new GraphQLRequest(IntrospectionQuery.INTROSPECTION_QUERY, new HashMap<>(), null);
    private static final String HANDSHAKE_REQUEST_KEY = HandshakeRequest.class.getName();

    protected abstract GraphQLSchemaProvider getSchemaProvider();
    protected abstract GraphQLContextBuilder getContextBuilder();
    protected abstract GraphQLRootObjectBuilder getRootObjectBuilder();
    protected abstract ExecutionStrategyProvider getExecutionStrategyProvider();
    protected abstract Instrumentation getInstrumentation();

    protected abstract GraphQLErrorHandler getGraphQLErrorHandler();
    protected abstract PreparsedDocumentProvider getPreparsedDocumentProvider();

    private final LazyObjectMapperBuilder lazyObjectMapperBuilder;
    private final List<GraphQLServletListener> listeners;
    private final ServletFileUpload fileUpload;
    private final GraphQLRequestInfoFactory requestInfoFactory;

    private final HttpRequestHandler getHandler;
    private final HttpRequestHandler postHandler;

    public GraphQLServlet() {
        this(null, null, null);
    }

    public GraphQLServlet(ObjectMapperConfigurer objectMapperConfigurer, List<GraphQLServletListener> listeners, FileItemFactory fileItemFactory) {
        this.lazyObjectMapperBuilder = new LazyObjectMapperBuilder(objectMapperConfigurer != null ? objectMapperConfigurer : new DefaultObjectMapperConfigurer());
        this.listeners = listeners != null ? new ArrayList<>(listeners) : new ArrayList<>();
        this.fileUpload = new ServletFileUpload(fileItemFactory != null ? fileItemFactory : new DiskFileItemFactory());
        this.requestInfoFactory = new GraphQLRequestInfoFactory(
            this::getSchemaProvider,
            this::getContextBuilder,
            this::getRootObjectBuilder
        );

        this.getHandler = (request, response) -> {
            String path = request.getPathInfo();
            if (path == null) {
                path = request.getServletPath();
            }
            if (path.contentEquals("/schema.json")) {
                doQuery(INTROSPECTION_REQUEST, requestInfoFactory.create(request), response);
            } else {
                String query = request.getParameter("query");
                if (query != null) {
                    GraphQLRequestInfo info = requestInfoFactory.createReadOnly(request);

                    if (isBatchedQuery(query)) {
                        doBatchedQuery(getGraphQLRequestMapper().readValues(query), info, response);
                    } else {
                        final Map<String, Object> variables = new HashMap<>();
                        if (request.getParameter("variables") != null) {
                            variables.putAll(deserializeVariables(request.getParameter("variables")));
                        }

                        String operationName = null;
                        if (request.getParameter("operationName") != null) {
                            operationName = request.getParameter("operationName");
                        }

                        doQuery(new GraphQLRequest(query, variables, operationName), info, response);
                    }
                } else {
                    response.setStatus(STATUS_BAD_REQUEST);
                    log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
                }
            }
        };

        this.postHandler = (request, response) -> {
            final GraphQLRequestInfo info = requestInfoFactory.create(request);

            try {
                if (ServletFileUpload.isMultipartContent(request)) {
                    final Map<String, List<FileItem>> fileItems = fileUpload.parseParameterMap(request);
                    info.getContext().setFiles(fileItems);

                    if (fileItems.containsKey("graphql")) {
                        final Optional<FileItem> graphqlItem = getFileItem(fileItems, "graphql");
                        if (graphqlItem.isPresent()) {
                            InputStream inputStream = graphqlItem.get().getInputStream();

                            if (!inputStream.markSupported()) {
                                inputStream = new BufferedInputStream(inputStream);
                            }

                            if (isBatchedQuery(inputStream)) {
                                doBatchedQuery(getGraphQLRequestMapper().readValues(inputStream), info, response);
                                return;
                            } else {
                                doQuery(getGraphQLRequestMapper().readValue(inputStream), info, response);
                                return;
                            }
                        }
                    } else if (fileItems.containsKey("query")) {
                        final Optional<FileItem> queryItem = getFileItem(fileItems, "query");
                        if (queryItem.isPresent()) {
                            InputStream inputStream = queryItem.get().getInputStream();

                            if (!inputStream.markSupported()) {
                                inputStream = new BufferedInputStream(inputStream);
                            }

                            if (isBatchedQuery(inputStream)) {
                                doBatchedQuery(getGraphQLRequestMapper().readValues(inputStream), info, response);
                                return;
                            } else {
                                String query = new String(queryItem.get().get());

                                Map<String, Object> variables = null;
                                final Optional<FileItem> variablesItem = getFileItem(fileItems, "variables");
                                if (variablesItem.isPresent()) {
                                    variables = deserializeVariables(new String(variablesItem.get().get()));
                                }

                                String operationName = null;
                                final Optional<FileItem> operationNameItem = getFileItem(fileItems, "operationName");
                                if (operationNameItem.isPresent()) {
                                    operationName = new String(operationNameItem.get().get()).trim();
                                }

                                doQuery(new GraphQLRequest(query, variables, operationName), info, response);
                                return;
                            }
                        }
                    }

                    response.setStatus(STATUS_BAD_REQUEST);
                    log.info("Bad POST multipart request: no part named \"graphql\" or \"query\"");
                } else {
                    // this is not a multipart request
                    InputStream inputStream = request.getInputStream();

                    if (!inputStream.markSupported()) {
                        inputStream = new BufferedInputStream(inputStream);
                    }

                    if (isBatchedQuery(inputStream)) {
                        doBatchedQuery(getGraphQLRequestMapper().readValues(inputStream), info, response);
                    } else {
                        doQuery(getGraphQLRequestMapper().readValue(inputStream), info, response);
                    }
                }
            } catch (Exception e) {
                log.info("Bad POST request: parsing failed", e);
                response.setStatus(STATUS_BAD_REQUEST);
            }
        };
    }

    protected ObjectMapper getMapper() {
        return lazyObjectMapperBuilder.getMapper();
    }

    /**
     * Creates an {@link ObjectReader} for deserializing {@link GraphQLRequest}
     */
    private ObjectReader getGraphQLRequestMapper() {
        // Add object mapper to injection so VariablesDeserializer can access it...
        InjectableValues.Std injectableValues = new InjectableValues.Std();
        injectableValues.addValue(ObjectMapper.class, getMapper());

        return getMapper().reader(injectableValues).forType(GraphQLRequest.class);
    }

    public void addListener(GraphQLServletListener servletListener) {
        listeners.add(servletListener);
    }

    public void removeListener(GraphQLServletListener servletListener) {
        listeners.remove(servletListener);
    }

    @Override
    public String[] getQueries() {
        return getSchemaProvider().getSchema().getQueryType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
    }

    @Override
    public String[] getMutations() {
        return getSchemaProvider().getSchema().getMutationType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
    }

    @Override
    public String executeQuery(String query) {
        try {
            final ExecutionResult result = newGraphQL(getSchemaProvider().getSchema()).execute(new ExecutionInput(query, null, getContextBuilder().build(), getRootObjectBuilder().build(), new HashMap<>()));
            return getMapper().writeValueAsString(createResultFromExecutionResult(result));
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private void doRequest(HttpServletRequest request, HttpServletResponse response, HttpRequestHandler handler) {

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
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doRequest(req, resp, getHandler);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doRequest(req, resp, postHandler);
    }

    private Optional<FileItem> getFileItem(Map<String, List<FileItem>> fileItems, String name) {
        List<FileItem> items = fileItems.get(name);
        if(items == null || items.isEmpty()) {
            return Optional.empty();
        }

        return items.stream().findFirst();
    }

    private GraphQL newGraphQL(GraphQLSchema schema) {
        ExecutionStrategyProvider executionStrategyProvider = getExecutionStrategyProvider();
        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(executionStrategyProvider.getQueryExecutionStrategy())
            .mutationExecutionStrategy(executionStrategyProvider.getMutationExecutionStrategy())
            .subscriptionExecutionStrategy(executionStrategyProvider.getSubscriptionExecutionStrategy())
            .instrumentation(getInstrumentation())
            .preparsedDocumentProvider(getPreparsedDocumentProvider())
            .build();
    }

    private void doQuery(GraphQLRequest graphQLRequest, GraphQLRequestInfo info, HttpServletResponse resp) throws Exception {
        query(graphQLRequest, info, serializeResultAsJson(response -> {
            resp.setContentType(APPLICATION_JSON_UTF8);
            resp.setStatus(STATUS_OK);
            resp.getWriter().write(response);
        }));
    }

    private void doBatchedQuery(Iterator<GraphQLRequest> graphQLRequests, GraphQLRequestInfo info, HttpServletResponse resp) throws Exception {
        resp.setContentType(APPLICATION_JSON_UTF8);
        resp.setStatus(STATUS_OK);

        Writer respWriter = resp.getWriter();
        respWriter.write('[');
        while (graphQLRequests.hasNext()) {
            GraphQLRequest graphQLRequest = graphQLRequests.next();
            query(graphQLRequest, info, serializeResultAsJson(respWriter::write));
            if (graphQLRequests.hasNext()) {
                respWriter.write(',');
            }
        }
        respWriter.write(']');
    }

    private void query(GraphQLRequest request, GraphQLRequestInfo info, ExecutionResultHandler resultHandler) throws Exception {
        if (request.getOperationName() != null && request.getOperationName().isEmpty()) {
            query(request.withoutOperationName(), info, resultHandler);
        } else if (Subject.getSubject(AccessController.getContext()) == null && info.getContext().getSubject().isPresent()) {
            Subject.doAs(info.getContext().getSubject().get(), (PrivilegedAction<Void>) () -> {
                try {
                    query(request, info, resultHandler);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        } else {
            String query = request.getQuery();
            Map<String, Object> variables = request.getVariables();
            String operationName = request.getOperationName();

            GraphQLSchema schema = info.getSchema();
            GraphQLContext context = info.getContext();
            Object rootObject = info.getRoot();

            List<GraphQLServletListener.OperationCallback> operationCallbacks = runListeners(l -> l.onOperation(context, operationName, query, variables));

            try {
                final ExecutionResult executionResult = newGraphQL(schema).execute(new ExecutionInput(query, operationName, context, rootObject, variables));

                resultHandler.accept(executionResult);

                if (getGraphQLErrorHandler().errorsPresent(executionResult.getErrors())) {
                    runCallbacks(operationCallbacks, c -> c.onError(context, operationName, query, variables, executionResult));
                } else {
                    runCallbacks(operationCallbacks, c -> c.onSuccess(context, operationName, query, variables, executionResult));
                }

            } finally {
                runCallbacks(operationCallbacks, c -> c.onFinally(context, operationName, query, variables));
            }
        }
    }

    private ExecutionResultHandler serializeResultAsJson(StringHandler responseHandler) {
        return executionResult -> responseHandler.accept(getMapper().writeValueAsString(createResultFromExecutionResult(executionResult)));
    }

    private Map<String, Object> createResultFromExecutionResult(ExecutionResult executionResult) {

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", executionResult.getData());

        if (getGraphQLErrorHandler().errorsPresent(executionResult.getErrors())) {
            result.put("errors", getGraphQLErrorHandler().processErrors(executionResult.getErrors()));
        }

        if(executionResult.getExtensions() != null){
            result.put("extensions", executionResult.getExtensions());
        }

        return result;
    }

    private <R> List<R> runListeners(Function<? super GraphQLServletListener, R> action) {
        if (listeners == null) {
            return Collections.emptyList();
        }

        return listeners.stream()
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

    private Map<String, Object> deserializeVariables(String variables) {
        try {
            return VariablesDeserializer.deserializeVariablesObject(getMapper().readValue(variables, Object.class), getMapper());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isBatchedQuery(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return false;
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[128];
        int length;

        inputStream.mark(0);
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
            String chunk = result.toString();
            Boolean isArrayStart = isArrayStart(chunk);
            if (isArrayStart != null) {
                inputStream.reset();
                return isArrayStart;
            }
        }

        inputStream.reset();
        return false;
    }

    private boolean isBatchedQuery(String query) {
        if (query == null) {
            return false;
        }

        Boolean isArrayStart = isArrayStart(query);
        return isArrayStart != null && isArrayStart;
    }

    // return true if the first non whitespace character is the beginning of an array
    private Boolean isArrayStart(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!Character.isWhitespace(ch)) {
                return ch == '[';
            }
        }

        return null;
    }

    /**
     * Must be used with {@link #modifyHandshake(ServerEndpointConfig, HandshakeRequest, HandshakeResponse)}
     * @return A websocket {@link Endpoint}
     */
    public Endpoint getWebsocketEndpoint() {
        return new Endpoint() {

            private final Map<Session, WsSessionSubscriptions> sessionSubscriptionCache = new HashMap<>();
            private final CloseReason ERROR_CLOSE_REASON = new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal Server Error");

            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {

                final WsSessionSubscriptions subscriptions = new WsSessionSubscriptions();
                final HandshakeRequest request = (HandshakeRequest) session.getUserProperties().get(HANDSHAKE_REQUEST_KEY);

                sessionSubscriptionCache.put(session, subscriptions);

                // This *cannot* be a lambda because of the way undertow checks the class...
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String text) {
                        try {
                            query(getGraphQLRequestMapper().readValue(text), requestInfoFactory.create(request), (executionResult) -> {
                                Object data = executionResult.getData();
//                                session.getBasicRemote().sendText();
                            });
                        } catch (Throwable t) {
                            log.error("Error executing websocket query for session: {}", session.getId(), t);
                            closeUnexpectedly(session, t);
                        }
                    }
                });
            }

            @Override
            public void onClose(Session session, CloseReason closeReason) {
                log.debug("Session closed: {}, {}", session.getId(), closeReason);
                WsSessionSubscriptions subscriptions = sessionSubscriptionCache.remove(session);
                if(subscriptions != null) {
                    subscriptions.close();
                }
            }

            @Override
            public void onError(Session session, Throwable thr) {
                log.error("Error in websocket session: {}", session.getId(), thr);
                closeUnexpectedly(session, thr);
            }

            private void closeUnexpectedly(Session session, Throwable t) {
                try {
                    session.close(ERROR_CLOSE_REASON);
                } catch (IOException e) {
                    log.error("Error closing websocket session for session: {}", session.getId(), t);
                }
            }
        };
    }

    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        sec.getUserProperties().put(HANDSHAKE_REQUEST_KEY, request);

        if(request.getHeaders().get(HandshakeResponse.SEC_WEBSOCKET_ACCEPT) != null) {
            response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, request.getHeaders().get(HandshakeResponse.SEC_WEBSOCKET_ACCEPT));
        }
        response.getHeaders().put(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL, Collections.singletonList("graphql-ws"));
    }

    protected interface HttpRequestHandler extends BiConsumer<HttpServletRequest, HttpServletResponse> {
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

    protected interface ExecutionResultHandler extends Consumer<ExecutionResult> {
        @Override
        default void accept(ExecutionResult executionResult) {
            try {
                handle(executionResult);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void handle(ExecutionResult result) throws Exception;
    }

    protected interface StringHandler extends Consumer<String> {
        @Override
        default void accept(String result) {
            try {
                handle(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void handle(String result) throws Exception;
    }
}
