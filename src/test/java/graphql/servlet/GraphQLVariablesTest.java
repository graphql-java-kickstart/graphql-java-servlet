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

import graphql.annotations.GraphQLAnnotations;
import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import lombok.SneakyThrows;
import lombok.Value;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GraphQLVariablesTest {

    public static class ComplexQueryProvider implements GraphQLQueryProvider {


        @Value
        public static class Data {
            @GraphQLField
            private String field1;
            @GraphQLField
            private String field2;
        }

        @Value
        public static class DataInput {
            @GraphQLField
            private String field1;
            @GraphQLField
            private String field2;
        }

        @GraphQLName("data")
        public static class DataQuery {
            @GraphQLField
            public Data echo(DataInput data) {
                return new Data(data.getField1(), data.getField2());
            }
        }

        @Override @SneakyThrows
        public GraphQLObjectType getQuery() {
            return GraphQLAnnotations.object(DataQuery.class);
        }

        @Override
        public Object context() {
            return new DataQuery();
        }
    }

    private static final String QUERY = "query Q($d: Data) { data { echo(data: $d) { field1 field2 } } }";

    @Test
    public void variableTyping() {
        GraphQLServlet servlet = new GraphQLServlet();
        ComplexQueryProvider queryProvider = new ComplexQueryProvider();
        servlet.bindQueryProvider(queryProvider);
        GraphQLSchema schema = servlet.getSchema();
        HashMap<String, Object> data = new HashMap<>();
        data.put("field1", "1");
        data.put("field2", "2");
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("d", data);
        GraphQLVariables variables = new GraphQLVariables(schema, QUERY, vars);
        Object d = variables.get("d");
        assertTrue(d instanceof ComplexQueryProvider.Data);
        assertEquals(((ComplexQueryProvider.Data)d).getField1(), "1");
        assertEquals(((ComplexQueryProvider.Data)d).getField2(), "2");
    }

    private static final String NON_NULL_QUERY = "query Q($d: Data!) { data { echo(data: $d) { field1 field2 } } }";

    @Test
    public void nonNullvariableTyping() {
        GraphQLServlet servlet = new GraphQLServlet();
        ComplexQueryProvider queryProvider = new ComplexQueryProvider();
        servlet.bindQueryProvider(queryProvider);
        GraphQLSchema schema = servlet.getSchema();
        HashMap<String, Object> data = new HashMap<>();
        data.put("field1", "1");
        data.put("field2", "2");
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("d", data);
        GraphQLVariables variables = new GraphQLVariables(schema, NON_NULL_QUERY, vars);
        Object d = variables.get("d");
        assertTrue(d instanceof ComplexQueryProvider.Data);
        assertEquals(((ComplexQueryProvider.Data)d).getField1(), "1");
        assertEquals(((ComplexQueryProvider.Data)d).getField2(), "2");
    }

}