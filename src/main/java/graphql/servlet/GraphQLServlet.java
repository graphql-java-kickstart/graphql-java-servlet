/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package graphql.servlet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.io.CharStreams;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.InvalidSyntaxError;
import graphql.execution.ExecutionStrategy;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.security.auth.Subject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Andrew Potter
 */
@Slf4j
public abstract class GraphQLServlet extends HttpServlet implements Servlet, GraphQLMBean, GraphQLSchemaProvider {

    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final int STATUS_OK = 200;
    public static final int STATUS_BAD_REQUEST = 400;

    private static final ObjectMapper mapper = new ObjectMapper();

    protected abstract GraphQLContext createContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response);
    protected abstract ExecutionStrategy getExecutionStrategy();
    protected abstract Map<String, Object> transformVariables(GraphQLSchema schema, String query, Map<String, Object> variables);

    private final List<GraphQLOperationListener> operationListeners;
    private final List<GraphQLServletListener> servletListeners;

    public GraphQLServlet() {
        this(null, null);
    }

    public GraphQLServlet(List<GraphQLOperationListener> operationListeners, List<GraphQLServletListener> servletListeners) {
        this.operationListeners = operationListeners != null ? new ArrayList<>(operationListeners) : new ArrayList<>();
        this.servletListeners = servletListeners != null ? new ArrayList<>(servletListeners) : new ArrayList<>();
    }

    public void addOperationListener(GraphQLOperationListener operationListener) {
        operationListeners.add(operationListener);
    }

    public void removeOperationListener(GraphQLOperationListener operationListener) {
        operationListeners.remove(operationListener);
    }

    public void addServletListener(GraphQLServletListener servletListener) {
        servletListeners.add(servletListener);
    }

    public void removeServletListener(GraphQLServletListener servletListener) {
        servletListeners.remove(servletListener);
    }

    @Override
    public String[] getQueries() {
        return getSchema().getQueryType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
    }

    @Override
    public String[] getMutations() {
        return getSchema().getMutationType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
    }

    @Override @SneakyThrows
    public String executeQuery(String query) {
        try {
            ExecutionResult result = new GraphQL(getSchema()).execute(query, createContext(Optional.empty(), Optional.empty()), new HashMap<>());
            return mapper.writeValueAsString(createResultFromDataAndErrors(result.getData(), result.getErrors()));
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private final RequestHandler getHandler = (request, response) -> {
        GraphQLContext context = createContext(Optional.of(request), Optional.of(response));
        String path = request.getPathInfo();
        if (path == null) {
            path = request.getServletPath();
        }
        if (path.contentEquals("/schema.json")) {
            query(CharStreams.toString(new InputStreamReader(GraphQLServlet.class.getResourceAsStream("introspectionQuery"))), null, new HashMap<>(), getSchema(), request, response, context);
        } else {
            if (request.getParameter("query") != null) {
                Map<String, Object> variables = new HashMap<>();
                if (request.getParameter("variables") != null) {
                    variables.putAll(mapper.readValue(request.getParameter("variables"), new TypeReference<Map<String, Object>>() { }));
                }
                String operationName = null;
                if (request.getParameter("operationName") != null) {
                    operationName = request.getParameter("operationName");
                }
                query(request.getParameter("query"), operationName, variables, getReadOnlySchema(), request, response, context);
            } else {
                response.setStatus(STATUS_BAD_REQUEST);
                log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
            }
        }
    };

    private final RequestHandler postHandler = (request, response) -> {
        GraphQLContext context = createContext(Optional.of(request), Optional.of(response));
        InputStream inputStream = null;

        if (ServletFileUpload.isMultipartContent(request)) {
            Part part = request.getPart("graphql");
            if(part != null) {
                inputStream = part.getInputStream();
            }

            if (inputStream == null) {
                response.setStatus(STATUS_BAD_REQUEST);
                log.info("Bad POST multipart request: no part named \"graphql\"");
                return;
            }

            context.setParts(Optional.of(request.getParts()));

        } else {
            // this is not a multipart request
            inputStream = request.getInputStream();
        }

        GraphQLRequest graphQLRequest;
        try {
            graphQLRequest = mapper.readValue(inputStream, GraphQLRequest.class);
        } catch (Exception e) {
            log.info("Bad POST request: deserialization failed", e);
            response.setStatus(STATUS_BAD_REQUEST);
            return;
        }

        Map<String,Object> variables = graphQLRequest.variables;
        if (variables == null) {
            variables = new HashMap<>();
        }
        query(graphQLRequest.query, graphQLRequest.operationName, variables, getSchema(), request, response, context);
    };

    private void doRequest(HttpServletRequest request, HttpServletResponse response, RequestHandler handler) {
        try {
            runListeners(servletListeners, l -> l.onStart(request, response));
            handler.handle(request, response);

        } catch (Throwable t) {
            response.setStatus(500);
            log.error("Error executing GraphQL request!", t);
            runListeners(servletListeners, l -> l.onError(request, response, t));
        } finally {
            runListeners(servletListeners, l -> l.onFinally(request, response));
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

    private void query(String query, String operationName, Map<String, Object> variables, GraphQLSchema schema, HttpServletRequest req, HttpServletResponse resp, GraphQLContext context) throws IOException {
        if (Subject.getSubject(AccessController.getContext()) == null && context.getSubject().isPresent()) {
            Subject.doAs(context.getSubject().get(), new PrivilegedAction<Void>() {
                @Override @SneakyThrows
                public Void run() {
                    query(query, operationName, variables, schema, req, resp, context);
                    return null;
                }
            });
        } else {
            runListeners(operationListeners, l -> runListener(l, it -> it.beforeGraphQLOperation(context, operationName, query, variables)));

            ExecutionResult executionResult = new GraphQL(schema, getExecutionStrategy()).execute(query, operationName, context, transformVariables(schema, query, variables));
            List<GraphQLError> errors = executionResult.getErrors();
            Object data = executionResult.getData();

            String response = mapper.writeValueAsString(createResultFromDataAndErrors(data, errors));

            resp.setContentType(APPLICATION_JSON_UTF8);
            resp.setStatus(STATUS_OK);
            resp.getWriter().write(response);

            if(errorsPresent(errors)) {
                runListeners(operationListeners, l -> l.onFailedGraphQLOperation(context, operationName, query, variables, data, errors));
            } else {
                runListeners(operationListeners, l -> l.onSuccessfulGraphQLOperation(context, operationName, query, variables, data));
            }
        }
    }

    private Map<String, Object> createResultFromDataAndErrors(Object data, List<GraphQLError> errors) {

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);

        if (errorsPresent(errors)) {
            List<GraphQLError> clientErrors = filterGraphQLErrors(errors);
            if(clientErrors.size() < errors.size()) {
                // Some errors were filtered out to hide implementation - put a generic error in place.
                clientErrors.add(new GenericGraphQLError("Internal Server Error(s) while executing query"));
            }
            result.put("errors", clientErrors);
        }

        return result;
    }

    private boolean errorsPresent(List<GraphQLError> errors) {
        return errors != null && !errors.isEmpty();
    }

    protected List<GraphQLError> filterGraphQLErrors(List<GraphQLError> errors) {
        return errors.stream().
                filter(error -> error instanceof InvalidSyntaxError || error instanceof ValidationError).
                collect(Collectors.toList());
    }

    private <T> void runListeners(List<T> listeners, Consumer<? super T> action) {
        if(listeners != null) {
            listeners.forEach(l -> runListener(l, action));
        }
    }

    /**
     * Don't let listener errors escape to the client.
     */
    private <T> void runListener(T listener, Consumer<? super T> action) {
        try {
            action.accept(listener);
        } catch (Throwable t) {
            log.error("Error running listener: {}", listener.getClass().getName(), t);
        }
    }

    protected static class VariablesDeserializer extends JsonDeserializer<Map<String, Object>> {
        @Override
        public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Object o = p.readValueAs(Object.class);
            if (o instanceof Map) {
                return (Map<String, Object>) o;
            } else if (o instanceof String) {
                return mapper.readValue((String) o, new TypeReference<Map<String, Object>>() {});
            } else {
                throw new RuntimeJsonMappingException("variables should be either an object or a string");
            }
        }
    }

    protected static class GraphQLRequest {
        @Getter
        @Setter
        private String query;
        @Getter @Setter @JsonDeserialize(using = GraphQLServlet.VariablesDeserializer.class)
        private Map<String, Object> variables = new HashMap<>();
        @Getter @Setter
        private String operationName;
    }

    protected interface RequestHandler extends BiConsumer<HttpServletRequest, HttpServletResponse> {
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
}
