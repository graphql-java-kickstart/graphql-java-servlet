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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.annotations.GraphQLAnnotations;
import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import lombok.SneakyThrows;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static graphql.Scalars.GraphQLInt;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class GraphQLServletTest {

    public static class TestQueryProvider implements GraphQLQueryProvider {


        @GraphQLName("query")
        public static class Query {
            @GraphQLField
            public String field;
        }

        @Override @SneakyThrows
        public GraphQLObjectType getQuery() {
            return GraphQLAnnotations.object(Query.class);
        }

        @Override
        public Object context() {
            return new Query();
        }
    }

    @Test
    public void queryProvider() {
        GraphQLServlet servlet = new GraphQLServlet();
        TestQueryProvider queryProvider = new TestQueryProvider();
        servlet.bindQueryProvider(queryProvider);
        GraphQLFieldDefinition query = servlet.schema.getQueryType().getFieldDefinition("query");
        assertEquals(query.getType().getName(), "query");
        query = servlet.readOnlySchema.getQueryType().getFieldDefinition("query");
        assertEquals(query.getType().getName(), "query");
        servlet.unbindQueryProvider(queryProvider);
        assertTrue(servlet.schema.getQueryType().getFieldDefinitions().isEmpty());
        assertTrue(servlet.readOnlySchema.getQueryType().getFieldDefinitions().isEmpty());
    }

    public static class TestMutationProvider implements GraphQLMutationProvider {

        @Override
        public Collection<GraphQLFieldDefinition> getMutations() {
            return Collections.singletonList(newFieldDefinition().name("int").type(GraphQLInt).staticValue(1).build());
        }
    }

    @Test
    public void mutationProvider() {
        GraphQLServlet servlet = new GraphQLServlet();
        TestMutationProvider mutationProvider = new TestMutationProvider();
        servlet.bindMutationProvider(mutationProvider);
        assertTrue(servlet.schema.getMutationType().getFieldDefinition("int").getType().equals(GraphQLInt));
        assertNull(servlet.readOnlySchema.getMutationType());
        servlet.unbindMutationProvider(mutationProvider);
        assertTrue(servlet.schema.getMutationType().getFieldDefinitions().isEmpty());
    }

    @Test @SneakyThrows
    public void schema() {
        GraphQLServlet servlet = new GraphQLServlet();

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/schema.json");
        HttpServletResponse resp = mock(HttpServletResponse.class);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);
        when(resp.getWriter()).thenReturn(writer);

        verify(resp, times(0)).setStatus(anyInt());

        servlet.bindQueryProvider(new TestQueryProvider());
        servlet.doGet(req, resp);

        writer.flush();

        Map<String, Object> response = new ObjectMapper().readValue(outputStream.toByteArray(), new TypeReference<Map<String, Object>>() {});
        assertTrue(response.containsKey("data"));
        assertFalse(response.containsKey("errors"));
    }

}
