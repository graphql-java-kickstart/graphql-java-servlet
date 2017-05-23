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
package graphql.servlet

import graphql.annotations.GraphQLAnnotations
import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLName
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition

class GraphQLVariablesSpec extends Specification {

    static class ComplexQueryProvider implements GraphQLQueryProvider {

        @Override
        Collection<GraphQLFieldDefinition> getQueries() {
            List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();
            fieldDefinitions.add(newFieldDefinition()
                    .name("data")
                    .type(GraphQLAnnotations.object(DataQuery.class))
                    .staticValue(new DataQuery())
                    .build());
            return fieldDefinitions;
        }

        static class Data {
            @GraphQLField
            String field1
            @GraphQLField
            String field2
        }

        static class DataInput {
            @GraphQLField
            String field1
            @GraphQLField
            String field2
        }

        @GraphQLName("data")
        static class DataQuery {
            @GraphQLField
            Data echo(DataInput data) {
                return new Data(field1: data.field1, field2: data.field2)
            }
        }

    }

    GraphQLSchema schema

    def setup() {
        OsgiGraphQLServlet servlet = new OsgiGraphQLServlet()
        ComplexQueryProvider queryProvider = new ComplexQueryProvider()
        servlet.bindQueryProvider(queryProvider)
        schema = servlet.getSchemaProvider().getSchema()
    }

    private static final String QUERY = 'query Q($d: Data) { data { echo(data: $d) { field1 field2 } } }'

    def "variables are automatically coerced into correct specific types"() {
        when:
            GraphQLVariables variables = new GraphQLVariables(schema, QUERY, [
                d: [
                    field1: "1",
                    field2: "2"
                ]
            ])
            Object d = variables.get("d")

        then:
            d != null
            d instanceof ComplexQueryProvider.Data
            ((ComplexQueryProvider.Data) d).getField1() == "1"
            ((ComplexQueryProvider.Data) d).getField2() == "2"
    }

    private static final String NON_NULL_QUERY = 'query Q($d: Data!) { data { echo(data: $d) { field1 field2 } } }'

    def "non-null variables are automatically coerced into correct specific types"() {
        when:
            GraphQLVariables variables = new GraphQLVariables(schema, NON_NULL_QUERY, [
                d: [
                    field1: "1",
                    field2: "2"
                ]
            ])
            Object d = variables.get("d")

        then:
            d != null
            d instanceof ComplexQueryProvider.Data
            ((ComplexQueryProvider.Data) d).getField1() == "1"
            ((ComplexQueryProvider.Data) d).getField2() == "2"
    }
}