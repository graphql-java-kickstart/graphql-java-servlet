package graphql.servlet

import graphql.annotations.annotationTypes.GraphQLField
import graphql.annotations.annotationTypes.GraphQLName
import graphql.annotations.processor.GraphQLAnnotations
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
            query = servlet.getSchemaProvider().getSchema().getQueryType().getFieldDefinition("query")
        then:
            query.getType().getName() == "query"

        when:
            query = servlet.getSchemaProvider().getReadOnlySchema(null).getQueryType().getFieldDefinition("query")
        then:
            query.getType().getName() == "query"

        when:
            servlet.unbindQueryProvider(queryProvider)
        then:
            servlet.getSchemaProvider().getSchema().getQueryType().getFieldDefinitions().isEmpty()
            servlet.getSchemaProvider().getReadOnlySchema(null).getQueryType().getFieldDefinitions().isEmpty()
    }

    static class TestMutationProvider implements GraphQLMutationProvider {
        @Override
        Collection<GraphQLFieldDefinition> getMutations() {
            return Collections.singletonList(newFieldDefinition().name("int").type(GraphQLInt).staticValue(1).build())
        }
    }

    def "mutation provider adds mutation objects"() {
        setup:
            OsgiGraphQLServlet servlet = new OsgiGraphQLServlet()
            TestMutationProvider mutationProvider = new TestMutationProvider()

        when:
            servlet.bindMutationProvider(mutationProvider)
        then:
            servlet.getSchemaProvider().getSchema().getMutationType().getFieldDefinition("int").getType() == GraphQLInt
            servlet.getSchemaProvider().getReadOnlySchema(null).getMutationType() == null

        when:
            servlet.unbindMutationProvider(mutationProvider)
        then:
            servlet.getSchemaProvider().getSchema().getMutationType() == null
    }
}
