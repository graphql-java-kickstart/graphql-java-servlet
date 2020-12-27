package graphql.kickstart.servlet

import graphql.AssertException
import graphql.Scalars
import graphql.annotations.annotationTypes.GraphQLField
import graphql.annotations.annotationTypes.GraphQLName
import graphql.annotations.processor.GraphQLAnnotations
import graphql.kickstart.servlet.osgi.GraphQLCodeRegistryProvider
import graphql.kickstart.servlet.osgi.GraphQLMutationProvider
import graphql.kickstart.servlet.osgi.GraphQLQueryProvider
import graphql.kickstart.servlet.osgi.GraphQLSubscriptionProvider
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInterfaceType
import spock.lang.Specification

import java.lang.annotation.Annotation

import static graphql.Scalars.GraphQLInt
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition

class OsgiGraphQLHttpServletSpec extends Specification {

    static class TestQueryProvider implements GraphQLQueryProvider {

        @Override
        Collection<GraphQLFieldDefinition> getQueries() {
            List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();
            fieldDefinitions.add(newFieldDefinition()
                    .name("query")
                    .type(new GraphQLAnnotations().object(Query.class))
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
        OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
        TestQueryProvider queryProvider = new TestQueryProvider()
        servlet.bindQueryProvider(queryProvider)
        GraphQLFieldDefinition query

        when:
        query = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType().getFieldDefinition("query")
        then:
        query.getType().name == "query"

        when:
        query = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema(null).getQueryType().getFieldDefinition("query")
        then:
        query.getType().name == "query"

        when:
        servlet.unbindQueryProvider(queryProvider)
        then:
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType().getFieldDefinitions().get(0).name == "_empty"
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema(null).getQueryType().getFieldDefinitions().get(0).name == "_empty"
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
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getMutationType().getFieldDefinition("int").getType() == GraphQLInt
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema(null).getMutationType() == null

        when:
        servlet.unbindMutationProvider(mutationProvider)
        then:
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getMutationType() == null
    }

    static class TestSubscriptionProvider implements GraphQLSubscriptionProvider {
        @Override
        Collection<GraphQLFieldDefinition> getSubscriptions() {
            return Collections.singletonList(newFieldDefinition().name("subscription").type(new GraphQLAnnotations().object(Subscription.class)).build())
        }

        @GraphQLName("subscription")
        static class Subscription {
            @GraphQLField
            public String field;
        }
    }

    def "subscription provider adds subscription objects"() {
        setup:
        OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
        TestSubscriptionProvider subscriptionProvider = new TestSubscriptionProvider()
        servlet.bindSubscriptionProvider(subscriptionProvider)
        GraphQLFieldDefinition subscription

        when:
        subscription = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getSubscriptionType().getFieldDefinition("subscription")
        then:
        subscription.getType().getName() == "subscription"

        when:
        subscription = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema(null).getSubscriptionType().getFieldDefinition("subscription")
        then:
        subscription.getType().getName() == "subscription"

        when:
        servlet.unbindSubscriptionProvider(subscriptionProvider)
        then:
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getSubscriptionType() == null
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
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getCodeRegistry().getTypeResolver(GraphQLInterfaceType.newInterface().name("Type").build())
        then:
        notThrown AssertException

        when:
        servlet.unbindCodeRegistryProvider(codeRegistryProvider)
        servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getCodeRegistry().getTypeResolver(GraphQLInterfaceType.newInterface().name("Type").build())
        then:
        thrown AssertException
    }

    def "schema update delay throws no exception"() {
        setup:
        OsgiGraphQLHttpServlet servlet = new OsgiGraphQLHttpServlet()
        def config = Mock(OsgiGraphQLHttpServlet.Config)

        when:
        config.schema_update_delay() >> 1
        servlet.activate(config)
        servlet.updateSchema()
        servlet.updateSchema()
        servlet.deactivate()

        then:
        noExceptionThrown()
    }

    def "bind query provider adds query objects"() {
        setup:
        def servlet = new OsgiGraphQLHttpServlet()
        def queryProvider = new TestQueryProvider()
        def query

        when:
        servlet.bindProvider(queryProvider)
        query = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType().getFieldDefinition("query")

        then:
        query.getType().name == "query"

        when:
        query = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema(null).getQueryType().getFieldDefinition("query")

        then:
        query.getType().name == "query"
    }
}
