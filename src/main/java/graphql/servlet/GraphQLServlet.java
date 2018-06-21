package graphql.servlet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.stream.Stream;

/**
 * @author Andrew Potter
 */
public abstract class GraphQLServlet extends HttpServlet implements Servlet, GraphQLMBean {

    public static final Logger log = LoggerFactory.getLogger(GraphQLServlet.class);

    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final String APPLICATION_GRAPHQL = "application/graphql";
    public static final int STATUS_OK = 200;
    public static final int STATUS_BAD_REQUEST = 400;

    protected abstract GraphQLSchemaProvider getSchemaProvider();
    protected abstract GraphQLContext createContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response);
    protected abstract Object createRootObject(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response);
    protected abstract ExecutionStrategyProvider getExecutionStrategyProvider();
    protected abstract Instrumentation getInstrumentation();

    protected abstract GraphQLErrorHandler getGraphQLErrorHandler();
    protected abstract PreparsedDocumentProvider getPreparsedDocumentProvider();

    private final LazyObjectMapperBuilder lazyObjectMapperBuilder;
    private final List<GraphQLServletListener> listeners;

    private final HttpRequestHandler getHandler;
    private final HttpRequestHandler postHandler;

    public GraphQLServlet() {
        this(null, null);
    }

    public GraphQLServlet(ObjectMapperConfigurer objectMapperConfigurer, List<GraphQLServletListener> listeners) {
        this.lazyObjectMapperBuilder = new LazyObjectMapperBuilder(objectMapperConfigurer != null ? objectMapperConfigurer : new DefaultObjectMapperConfigurer());
        this.listeners = listeners != null ? new ArrayList<>(listeners) : new ArrayList<>();

        this.getHandler = (request, response) -> {
            final GraphQLContext context = createContext(Optional.of(request), Optional.of(response));
            final Object rootObject = createRootObject(Optional.of(request), Optional.of(response));

            String path = request.getPathInfo();
            if (path == null) {
                path = request.getServletPath();
            }
            if (path.contentEquals("/schema.json")) {
                doQuery(IntrospectionQuery.INTROSPECTION_QUERY, null, new HashMap<>(), getSchemaProvider().getSchema(request), context, rootObject, request, response);
            } else {
                String query = request.getParameter("query");
                if (query != null) {
                    if (isBatchedQuery(query)) {
                        doBatchedQuery(getGraphQLRequestMapper().readValues(query), getSchemaProvider().getReadOnlySchema(request), context, rootObject, request, response);
                    } else {
                        final Map<String, Object> variables = new HashMap<>();
                        if (request.getParameter("variables") != null) {
                            variables.putAll(deserializeVariables(request.getParameter("variables")));
                        }

                        String operationName = null;
                        if (request.getParameter("operationName") != null) {
                            operationName = request.getParameter("operationName");
                        }

                        doQuery(query, operationName, variables, getSchemaProvider().getReadOnlySchema(request), context, rootObject, request, response);
                    }
                } else {
                    response.setStatus(STATUS_BAD_REQUEST);
                    log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
                }
            }
        };

        this.postHandler = (request, response) -> {
            final GraphQLContext context = createContext(Optional.of(request), Optional.of(response));
            final Object rootObject = createRootObject(Optional.of(request), Optional.of(response));

            try {
                Collection<Part> parts = request.getParts();
                if (APPLICATION_GRAPHQL.equals(request.getContentType())) {
                    String query = CharStreams.toString(request.getReader());
                    doQuery(query, null, null, getSchemaProvider().getSchema(request), context, rootObject, request, response);
                } else if (!parts.isEmpty()) {
                    final Map<String, List<Part>> fileItems = parts.stream()
                            .collect(Collectors.toMap(
                                    Part::getName,
                                    Collections::singletonList,
                                    (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

                    context.setFiles(Optional.of(fileItems));

                    if (fileItems.containsKey("graphql")) {
                        final Optional<Part> graphqlItem = getFileItem(fileItems, "graphql");
                        if (graphqlItem.isPresent()) {
                            InputStream inputStream = graphqlItem.get().getInputStream();

                            if (!inputStream.markSupported()) {
                                inputStream = new BufferedInputStream(inputStream);
                            }

                            if (isBatchedQuery(inputStream)) {
                                doBatchedQuery(getGraphQLRequestMapper().readValues(inputStream), getSchemaProvider().getSchema(request), context, rootObject, request, response);
                                return;
                            } else {
                                doQuery(getGraphQLRequestMapper().readValue(inputStream), getSchemaProvider().getSchema(request), context, rootObject, request, response);
                                return;
                            }
                        }
                    } else if (fileItems.containsKey("query")) {
                        final Optional<Part> queryItem = getFileItem(fileItems, "query");
                        if (queryItem.isPresent()) {
                            InputStream inputStream = queryItem.get().getInputStream();

                            if (!inputStream.markSupported()) {
                                inputStream = new BufferedInputStream(inputStream);
                            }

                            if (isBatchedQuery(inputStream)) {
                                doBatchedQuery(getGraphQLRequestMapper().readValues(inputStream), getSchemaProvider().getSchema(request), context, rootObject, request, response);
                                return;
                            } else {

                                String query = new String(ByteStreams.toByteArray(inputStream));

                                Map<String, Object> variables = null;
                                final Optional<Part> variablesItem = getFileItem(fileItems, "variables");
                                if (variablesItem.isPresent()) {
                                    variables = deserializeVariables(new String(ByteStreams.toByteArray(variablesItem.get().getInputStream())));
                                }

                                String operationName = null;
                                final Optional<Part> operationNameItem = getFileItem(fileItems, "operationName");
                                if (operationNameItem.isPresent()) {
                                    operationName = new String(ByteStreams.toByteArray(operationNameItem.get().getInputStream())).trim();
                                }

                                doQuery(query, operationName, variables, getSchemaProvider().getSchema(request), context, rootObject, request, response);
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
                        doBatchedQuery(getGraphQLRequestMapper().readValues(inputStream), getSchemaProvider().getSchema(request), context, rootObject, request, response);
                    } else {
                        doQuery(getGraphQLRequestMapper().readValue(inputStream), getSchemaProvider().getSchema(request), context, rootObject, request, response);
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
            final ExecutionResult result = newGraphQL(getSchemaProvider().getSchema()).execute(new ExecutionInput(query, null, createContext(Optional.empty(), Optional.empty()), createRootObject(Optional.empty(), Optional.empty()), new HashMap<>()));
            return getMapper().writeValueAsString(createResultFromDataErrorsAndExtensions(result.getData(), result.getErrors(), result.getExtensions()));
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

    private Optional<Part> getFileItem(Map<String, List<Part>> fileItems, String name) {
        return Optional.ofNullable(fileItems.get(name)).filter(list -> !list.isEmpty()).map(list -> list.get(0));
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

    private void doQuery(GraphQLRequest graphQLRequest, GraphQLSchema schema, GraphQLContext context, Object rootObject, HttpServletRequest httpReq, HttpServletResponse httpRes) throws Exception {
        doQuery(graphQLRequest.getQuery(), graphQLRequest.getOperationName(), graphQLRequest.getVariables(), schema, context, rootObject, httpReq, httpRes);
    }

    private void doQuery(String query, String operationName, Map<String, Object> variables, GraphQLSchema schema, GraphQLContext context, Object rootObject, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        query(query, operationName, variables, schema, context, rootObject, (r) -> {
            resp.setContentType(APPLICATION_JSON_UTF8);
            resp.setStatus(r.getStatus());
            resp.getWriter().write(r.getResponse());
        });
    }

    private void doBatchedQuery(Iterator<GraphQLRequest> graphQLRequests, GraphQLSchema schema, GraphQLContext context, Object rootObject, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType(APPLICATION_JSON_UTF8);
        resp.setStatus(STATUS_OK);

        Writer respWriter = resp.getWriter();
        respWriter.write('[');
        while (graphQLRequests.hasNext()) {
            GraphQLRequest graphQLRequest = graphQLRequests.next();
            query(graphQLRequest.getQuery(), graphQLRequest.getOperationName(), graphQLRequest.getVariables(), schema, context, rootObject, (r) -> respWriter.write(r.getResponse()));
            if (graphQLRequests.hasNext()) {
                respWriter.write(',');
            }
        }
        respWriter.write(']');
    }

    private void query(String query, String operationName, Map<String, Object> variables, GraphQLSchema schema, GraphQLContext context, Object rootObject, GraphQLResponseHandler responseHandler) throws Exception {
        if (operationName != null && operationName.isEmpty()) {
            query(query, null, variables, schema, context, rootObject, responseHandler);
        } else if (Subject.getSubject(AccessController.getContext()) == null && context.getSubject().isPresent()) {
            Subject.doAs(context.getSubject().get(), (PrivilegedAction<Void>) () -> {
                try {
                    query(query, operationName, variables, schema, context, rootObject, responseHandler);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        } else {
            List<GraphQLServletListener.OperationCallback> operationCallbacks = runListeners(l -> l.onOperation(context, operationName, query, variables));

            final ExecutionResult executionResult = newGraphQL(schema).execute(new ExecutionInput(query, operationName, context, rootObject, variables));
            final List<GraphQLError> errors = executionResult.getErrors();
            final Object data = executionResult.getData();
            final Object extensions = executionResult.getExtensions();

            final String response = getMapper().writeValueAsString(createResultFromDataErrorsAndExtensions(data, errors, extensions));

            GraphQLResponse graphQLResponse = new GraphQLResponse();
            graphQLResponse.setStatus(STATUS_OK);
            graphQLResponse.setResponse(response);
            responseHandler.handle(graphQLResponse);

            if(getGraphQLErrorHandler().errorsPresent(errors)) {
                runCallbacks(operationCallbacks, c -> c.onError(context, operationName, query, variables, data, errors, extensions));
            } else {
                runCallbacks(operationCallbacks, c -> c.onSuccess(context, operationName, query, variables, data, extensions));
            }

            runCallbacks(operationCallbacks, c -> c.onFinally(context, operationName, query, variables, data, extensions));
        }
    }

    private Map<String, Object> createResultFromDataErrorsAndExtensions(Object data, List<GraphQLError> errors, Object extensions) {

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);

        if (getGraphQLErrorHandler().errorsPresent(errors)) {
            result.put("errors", getGraphQLErrorHandler().processErrors(errors));
        }

        if(extensions != null){
            result.put("extensions", extensions);
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

    protected static class VariablesDeserializer extends JsonDeserializer<Map<String, Object>> {

        @Override
        public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return deserializeVariablesObject(p.readValueAs(Object.class), (ObjectMapper) ctxt.findInjectableValue(ObjectMapper.class.getName(), null, null));
        }
    }

    private Map<String, Object> deserializeVariables(String variables) {
        try {
            return deserializeVariablesObject(getMapper().readValue(variables, Object.class), getMapper());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> deserializeVariablesObject(Object variables, ObjectMapper mapper) {
        if (variables instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> genericVariables = (Map<String, Object>) variables;
            return genericVariables;
        } else if (variables instanceof String) {
            try {
                return mapper.readValue((String) variables, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("variables should be either an object or a string");
        }
    }

    private boolean isBatchedQuery(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return false;
        }

        final int BUFFER_LENGTH = 128;
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_LENGTH];
        int length;

        inputStream.mark(BUFFER_LENGTH);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class GraphQLRequest {
        private String query;
        @JsonDeserialize(using = GraphQLServlet.VariablesDeserializer.class)
        private Map<String, Object> variables = new HashMap<>();
        private String operationName;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }

        public String getOperationName() {
            return operationName;
        }

        public void setOperationName(String operationName) {
            this.operationName = operationName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class GraphQLResponse {
        private int status;
        private String response;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
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

    protected interface GraphQLResponseHandler extends Consumer<GraphQLResponse> {
        @Override
        default void accept(GraphQLResponse response) {
            try {
                handle(response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void handle(GraphQLResponse r) throws Exception;
    }
}
