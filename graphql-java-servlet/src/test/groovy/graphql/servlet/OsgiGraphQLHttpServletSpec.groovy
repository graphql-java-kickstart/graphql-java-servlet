package graphql.servlet

import graphql.AssertException
import graphql.annotations.annotationTypes.GraphQLField
import graphql.annotations.annotationTypes.GraphQLName
import graphql.annotations.processor.GraphQLAnnotations
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInterfaceType
import graphql.servlet.osgi.GraphQLCodeRegistryProvider
import graphql.servlet.osgi.GraphQLMutationProvider
import graphql.servlet.osgi.GraphQLQueryProvider
import graphql.servlet.osgi.GraphQLSubscriptionProvider
import spock.lang.Ignore
import spock.lang.Specification

import static graphql.Scalars.GraphQLInt
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition

class OsgiGraphQLHttpServletSpec extends Specification {

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

    @Ignore
    def "query provider adds query objects"() {
        setup:
        OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
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
            OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
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

    static class TestSubscriptionProvider implements GraphQLSubscriptionProvider {
        @Override
        Collection<GraphQLFieldDefinition> getSubscriptions() {
            return Collections.singletonList(newFieldDefinition().name("subscription").type(GraphQLAnnotations.object(Subscription.class)).build())
        }


        @GraphQLName("subscription")
        static class Subscription {
            @GraphQLField
            public String field;
        }
    }

    @Ignore
    def "subscription provider adds subscription objects"() {
        setup:
            OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
            TestSubscriptionProvider subscriptionProvider = new TestSubscriptionProvider()
            servlet.bindSubscriptionProvider(subscriptionProvider)
            GraphQLFieldDefinition subscription

        when:
            subscription = servlet.getSchemaProvider().getSchema().getSubscriptionType().getFieldDefinition("subscription")
        then:
            subscription.getType().getName() == "subscription"

        when:
            subscription = servlet.getSchemaProvider().getReadOnlySchema(null).getSubscriptionType().getFieldDefinition("subscription")
        then:
            subscription.getType().getName() == "subscription"

        when:
            servlet.unbindSubscriptionProvider(subscriptionProvider)
        then:
            servlet.getSchemaProvider().getSchema().getSubscriptionType() == null
    }

    static class TestCodeRegistryProvider implements GraphQLCodeRegistryProvider {
        @Override
        GraphQLCodeRegistry getCodeRegistry() {
            return GraphQLCodeRegistry.newCodeRegistry().typeResolver("Type", { env -> null }).build();
        }
    }

    def "code registry provider adds type resolver"() {
        setup:
            OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
            TestCodeRegistryProvider codeRegistryProvider = new TestCodeRegistryProvider()

        when:
            servlet.bindCodeRegistryProvider(codeRegistryProvider)
            servlet.getSchemaProvider().getSchema().getCodeRegistry().getTypeResolver(GraphQLInterfaceType.newInterface().name("Type").build())
        then:
            notThrown AssertException

        when:
            servlet.unbindCodeRegistryProvider(codeRegistryProvider)
            servlet.getSchemaProvider().getSchema().getCodeRegistry().getTypeResolver(GraphQLInterfaceType.newInterface().name("Type").build())
        then:
            thrown AssertException

    }
}
