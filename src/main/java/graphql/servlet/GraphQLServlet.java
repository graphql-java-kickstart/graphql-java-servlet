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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import graphql.*;
import graphql.annotations.EnhancedExecutionStrategy;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.security.auth.Subject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
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
        GraphQLObjectType.Builder mutationObject = newObject().name("mutation");

        for (GraphQLQueryProvider provider : queryProviders) {
            GraphQLObjectType query = provider.getQuery();
            object.field(newFieldDefinition().
                    type(query).
                    staticValue(provider.context()).
                    name(provider.getName()).
                    build());
        }

        for (GraphQLMutationProvider provider : mutationProviders) {
            provider.getMutations().forEach(mutationObject::field);
        }

        readOnlySchema = newSchema().query(object.build()).build();
        schema = newSchema().query(object.build()).mutation(mutationObject.build()).build();
    }

    public GraphQLServlet() {
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.add(queryProvider);
        updateSchema();
    }
    public void unbindQueryProvider(GraphQLQueryProvider queryProvider) {
        queryProviders.remove(queryProvider);
        updateSchema();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
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

    @Reference
    public void setContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }
    public void unsetContextProvider(GraphQLContextBuilder contextBuilder) {
        this.contextBuilder = new DefaultGraphQLContextBuilder();
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

    public static class Request {
        @Getter @Setter
        private String query;
        @Getter @Setter
        private Map<String, Object> variables = new HashMap<>();
        @Getter @Setter
        private String operationName;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().contentEquals("/schema.json")) {
            query(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("introspectionQuery"))), new HashMap<>(), schema, req, resp);
        } else {
            query(req.getParameter("q"), new HashMap<>(), readOnlySchema, req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Request request = new ObjectMapper().readValue(req.getInputStream(), Request.class);
        query(request.query, request.variables, schema, req, resp);
    }

    private void query(String query, Map<String, Object> variables, GraphQLSchema schema, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        GraphQLContext context = createContext(Optional.of(req), Optional.of(resp));
        if (Subject.getSubject(AccessController.getContext()) == null && context.getSubject().isPresent()) {
            Subject.doAs(context.getSubject().get(), new PrivilegedAction<Void>() {
                @Override @SneakyThrows
                public Void run() {
                    query(query, variables, schema, req, resp);
                    return null;
                }
            });
        } else {
            ExecutionResult result = new GraphQL(schema, new EnhancedExecutionStrategy()).execute(query, context, variables);
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
