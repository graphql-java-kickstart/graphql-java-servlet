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
import graphql.*;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import javax.security.auth.Subject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

@Slf4j
@Component(property = {"alias=/graphql", "jmx.objectname=graphql.servlet:type=graphql"})
public class GraphQLServlet extends HttpServlet implements Servlet, GraphQLMBean, GraphQLSchemaProvider {

    private List<GraphQLQueryProvider> queryProviders = new ArrayList<>();
    private List<GraphQLMutationProvider> mutationProviders = new ArrayList<>();

    @Getter
    GraphQLSchema schema;
    @Getter
    GraphQLSchema readOnlySchema;

    protected void updateSchema() {
        GraphQLObjectType.Builder object = newObject().name("query");

        for (GraphQLQueryProvider provider : queryProviders) {
            GraphQLObjectType query = provider.getQuery();
            object.field(newFieldDefinition().
                    type(query).
                    staticValue(provider.context()).
                    name(provider.getName()).
                    description(query.getDescription()).
                    build());
        }

        readOnlySchema = newSchema().query(object.build()).build();

        if (mutationProviders.isEmpty()) {
            schema = readOnlySchema;
        } else {
            GraphQLObjectType.Builder mutationObject = newObject().name("mutation");

            for (GraphQLMutationProvider provider : mutationProviders) {
                provider.getMutations().forEach(mutationObject::field);
            }

            GraphQLObjectType mutationType = mutationObject.build();
            if (mutationType.getFieldDefinitions().size() == 0) {
                schema = readOnlySchema;
            } else {
                schema = newSchema().query(object.build()).mutation(mutationType).build();
            }
        }
    }

    public GraphQLServlet() {
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.add(queryProvider);
        updateSchema();
    }
    public void unbindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.remove(queryProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    public void bindMutationProvider(GraphQLMutationProvider mutationProvider) {
        mutationProviders.add(mutationProvider);
        updateSchema();
    }
    public void unbindMutationProvider(GraphQLMutationProvider mutationProvider) {
        mutationProviders.remove(mutationProvider);
        updateSchema();
    }

    @Override
    public String[] getQueries() {
        return schema.getQueryType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
    }

    @Override
    public String[] getMutations() {
        return schema.getMutationType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
    }

    private GraphQLContextBuilder contextBuilder = new DefaultGraphQLContextBuilder();
    private ExecutionStrategyProvider executionStrategyProvider = new EnhancedExecutionStrategyProvider();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    public void setContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }
    public void unsetContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = new DefaultGraphQLContextBuilder();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setExecutionStrategyProvider(ExecutionStrategyProvider provider) {
        executionStrategyProvider = provider;
    }
    public void unsetExecutionStrategyProvider(ExecutionStrategyProvider provider) {
        executionStrategyProvider = new EnhancedExecutionStrategyProvider();
    }

    protected GraphQLContext createContext(Optional<HttpServletRequest> req, Optional<HttpServletResponse> resp) {
        return contextBuilder.build(req, resp);
    }

    @Override @SneakyThrows
    public String executeQuery(String query) {
        try {
            ExecutionResult result = new GraphQL(schema).execute(query, createContext(Optional.empty(), Optional.empty()), new HashMap<>());
            if (result.getErrors().isEmpty()) {
                return new ObjectMapper().writeValueAsString(result.getData());
            } else {
                return new ObjectMapper().writeValueAsString(getGraphQLErrors(result));
            }
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static class VariablesDeserializer extends JsonDeserializer<Map<String, Object>> {

        @Override public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            Object o = p.readValueAs(Object.class);
            if (o instanceof Map) {
                return (Map<String, Object>) o;
            } else if (o instanceof String) {
                return new ObjectMapper().readValue((String) o, new TypeReference<Map<String, Object>>() {});
            } else {
                throw new RuntimeJsonMappingException("variables should be either an object or a string");
            }
        }
    }

    public static class Request {
        @Getter @Setter
        private String query;
        @Getter @Setter @JsonDeserialize(using = VariablesDeserializer.class)
        private Map<String, Object> variables = new HashMap<>();
        @Getter @Setter
        private String operationName;

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        GraphQLContext context = createContext(Optional.of(req), Optional.of(resp));
        String path = req.getPathInfo();
        if (path == null) {
            path = req.getServletPath();
        }
        if (path.contentEquals("/schema.json")) {
            query(CharStreams.toString(new InputStreamReader(GraphQLServlet.class.getResourceAsStream("introspectionQuery"))), null, new HashMap<>(), schema, req, resp, context);
        } else {
            if (req.getParameter("q") != null) {
                query(req.getParameter("q"), null, new HashMap<>(), readOnlySchema, req, resp, context);
            } else if (req.getParameter("query") != null) {
                Map<String,Object> variables = new HashMap<>();
                if (req.getParameter("variables") != null) {
                    variables.putAll(new ObjectMapper().readValue(req.getParameter("variables"), new TypeReference<Map<String,Object>>() {}));
                }
                String operationName = null;
                if (req.getParameter("operationName") != null) {
                    operationName = req.getParameter("operationName");
                }
                query(req.getParameter("query"), operationName, variables, readOnlySchema, req, resp, context);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        GraphQLContext context = createContext(Optional.of(req), Optional.of(resp));
        InputStream inputStream = null;
        if (ServletFileUpload.isMultipartContent(req)) {
            ServletFileUpload upload = new ServletFileUpload();
            try {
                FileItemIterator it = upload.getItemIterator(req);
                context.setFiles(Optional.of(it));
                while (inputStream == null && it.hasNext()) {
                    FileItemStream stream = it.next();
                    if (stream.getFieldName().contentEquals("graphql")) {
                        inputStream = stream.openStream();
                    }
                }
                if (inputStream == null) {
                    throw new ServletException("no query found");
                }
            } catch (FileUploadException e) {
                throw new ServletException("no query found");
            }
        } else {
            // this is not a multipart request
            inputStream = req.getInputStream();
        }
        Request request = new ObjectMapper().readValue(inputStream, Request.class);
        Map<String,Object> variables = request.variables;
        if (variables == null) {
            variables = new HashMap<>();
        }
        query(request.query, request.operationName, variables, schema, req, resp, context);
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
            ExecutionResult result = new GraphQL(schema, executionStrategyProvider.getExecutionStrategy()).execute(query, operationName, context, variables);
            resp.setContentType("application/json");
            if (result.getErrors().isEmpty()) {
                Map<String, Object> dict = new HashMap<>();
                dict.put("data", result.getData());
                resp.getWriter().write(new ObjectMapper().writeValueAsString(dict));
            } else {
                result.getErrors().stream().
                        filter(error -> (error instanceof ExceptionWhileDataFetching)).
                        forEachOrdered(err -> log.error("{}", ((ExceptionWhileDataFetching) err).getException()));

                resp.setStatus(500);
                List<GraphQLError> errors = getGraphQLErrors(result);
                Map<String, Object> dict = new HashMap<>();
                dict.put("errors", errors);

                resp.getWriter().write(new ObjectMapper().writeValueAsString(dict));
            }
        }
    }

    private List<GraphQLError> getGraphQLErrors(ExecutionResult result) {
        return result.getErrors().stream().
                filter(error -> error instanceof InvalidSyntaxError || error instanceof ValidationError).
                collect(Collectors.toList());
    }
}
