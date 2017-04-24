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
import spock.lang.Specification

import static graphql.Scalars.GraphQLInt
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition

class OsgiGraphQLServletSpec extends Specification {

    static class TestQueryProvider implements GraphQLQueryProvider {

        @Override
        Collection<GraphQLFieldDefinition> getQueries() {
            List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();
            fieldDefinitions.add(newFieldDefinition()
                    .name("query")
                    .type(GraphQLAnnotations.object(Query.class))
                    .staticValue(new Query())
                    .build());
            return fieldDefinitions;
        }

        @GraphQLName("query")
        static class Query {
            @GraphQLField
            public String field;
        }

    }

    def "query provider adds query objects"() {
        setup:
            OsgiGraphQLServlet servlet = new OsgiGraphQLServlet()
            TestQueryProvider queryProvider = new TestQueryProvider()
            servlet.bindQueryProvider(queryProvider)
            GraphQLFieldDefinition query

        when:
            query = servlet.getSchema().getQueryType().getFieldDefinition("query")
        then:
            query.getType().getName() == "query"

        when:
            query = servlet.getReadOnlySchema().getQueryType().getFieldDefinition("query")
        then:
            query.getType().getName() == "query"

        when:
            servlet.unbindQueryProvider(queryProvider)
        then:
            servlet.getSchema().getQueryType().getFieldDefinitions().isEmpty()
            servlet.getReadOnlySchema().getQueryType().getFieldDefinitions().isEmpty()
    }

    static class TestMutationProvider implements GraphQLMutationProvider {
        @Override
        Collection<GraphQLFieldDefinition> getMutations() {
            return Collections.singletonList(newFieldDefinition().name("int").type(GraphQLInt).staticValue(1).build());
        }
    }

    def "mutation provider adds mutation objects"() {
        setup:
            OsgiGraphQLServlet servlet = new OsgiGraphQLServlet();
            TestMutationProvider mutationProvider = new TestMutationProvider();

        when:
            servlet.bindMutationProvider(mutationProvider)
        then:
            servlet.getSchema().getMutationType().getFieldDefinition("int").getType() == GraphQLInt
            servlet.getReadOnlySchema().getMutationType() == null

        when:
            servlet.unbindMutationProvider(mutationProvider)
        then:
            servlet.getSchema().getMutationType() == null
    }
}
