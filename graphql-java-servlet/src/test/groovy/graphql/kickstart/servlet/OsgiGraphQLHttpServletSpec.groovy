package graphql.kickstart.servlet

import graphql.AssertException
import graphql.annotations.annotationTypes.GraphQLField
import graphql.annotations.annotationTypes.GraphQLName
import graphql.annotations.processor.GraphQLAnnotations
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters
import graphql.kickstart.execution.GraphQLRequest
import graphql.kickstart.execution.config.ExecutionStrategyProvider
import graphql.kickstart.execution.config.InstrumentationProvider
import graphql.kickstart.execution.context.DefaultGraphQLContext
import graphql.kickstart.execution.context.GraphQLKickstartContext
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder
import graphql.kickstart.servlet.core.GraphQLServletListener
import graphql.kickstart.servlet.core.GraphQLServletRootObjectBuilder
import graphql.kickstart.servlet.osgi.*
import graphql.schema.*
import org.dataloader.DataLoaderRegistry
import spock.lang.Specification

import static graphql.Scalars.GraphQLInt
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition

class OsgiGraphQLHttpServletSpec extends Specification {

  static class TestQueryProvider implements GraphQLQueryProvider {

    @Override
    Collection<GraphQLFieldDefinition> getQueries() {
      List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>()
      fieldDefinitions.add(newFieldDefinition()
          .name("query")
          .type(new GraphQLAnnotations().object(Query.class))
          .staticValue(new Query())
          .build())
      return fieldDefinitions
    }

    @GraphQLName("query")
    static class Query {
      @GraphQLField
      public String field
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
    query = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getQueryType().getFieldDefinition("query")
    then:
    query.getType().name == "query"

    when:
    servlet.unbindQueryProvider(queryProvider)
    then:
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType().getFieldDefinitions().get(0).name == "_empty"
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getQueryType().getFieldDefinitions().get(0).name == "_empty"
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
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getMutationType() == null

    when:
    servlet.unbindMutationProvider(mutationProvider)
    then:
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getMutationType() == null

    when:
    servlet.bindProvider(mutationProvider)
    then:
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getMutationType().getFieldDefinition("int").getType() == GraphQLInt
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getMutationType() == null

    when:
    servlet.unbindProvider(mutationProvider)
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
      public String field
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
    subscription = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getSubscriptionType().getFieldDefinition("subscription")
    then:
    subscription.getType().getName() == "subscription"

    when:
    servlet.unbindSubscriptionProvider(subscriptionProvider)
    then:
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getSubscriptionType() == null

    when:
    servlet.bindProvider(subscriptionProvider)
    then:
    def subscription2 = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getSubscriptionType().getFieldDefinition("subscription")
    subscription2.getType().getName() == "subscription"

    when:
    servlet.unbindProvider(subscriptionProvider)
    then:
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getSubscriptionType() == null
  }

  static class TestCodeRegistryProvider implements GraphQLCodeRegistryProvider {
    @Override
    GraphQLCodeRegistry getCodeRegistry() {
      return GraphQLCodeRegistry.newCodeRegistry().typeResolver("Type", { env -> null }).build()
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

    when:
    servlet.bindProvider(codeRegistryProvider)
    servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getCodeRegistry().getTypeResolver(GraphQLInterfaceType.newInterface().name("Type").build())
    then:
    notThrown AssertException

    when:
    servlet.unbindProvider(codeRegistryProvider)
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
    query = servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getReadOnlySchema().getQueryType().getFieldDefinition("query")

    then:
    query.getType().name == "query"

    when:
    servlet.unbindProvider(queryProvider)
    then:
    null != servlet.getConfiguration().getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType().getFieldDefinition("_empty")
  }

  def "type provider adds types"() {
    setup:
    def servlet = new OsgiGraphQLHttpServlet()
    def typesProvider = Mock(GraphQLTypesProvider)
    def coercing = Mock(Coercing)
    typesProvider.types >> [GraphQLScalarType.newScalar().name("Upload").coercing(coercing).build()]

    when:
    servlet.bindTypesProvider(typesProvider)

    then:
    def type = servlet.configuration.invocationInputFactory.schemaProvider.schema.getType("Upload")
    type != null
    type.name == "Upload"
    type instanceof GraphQLScalarType
    def scalarType = (GraphQLScalarType) type
    scalarType.coercing == coercing

    when:
    servlet.unbindTypesProvider(typesProvider)

    then:
    null == servlet.configuration.invocationInputFactory.schemaProvider.schema.getType("Upload")

    when:
    servlet.bindProvider(typesProvider)
    then:
    servlet.configuration.invocationInputFactory.schemaProvider.schema.getType("Upload").name == "Upload"

    when:
    servlet.unbindProvider(typesProvider)
    then:
    null == servlet.configuration.invocationInputFactory.schemaProvider.schema.getType("Upload")
  }

  def "servlet listener is bound and unbound"() {
    setup:
    def servlet = new OsgiGraphQLHttpServlet()
    def listener = Mock(GraphQLServletListener)

    when:
    servlet.bindServletListener(listener)
    then:
    servlet.configuration.listeners.contains(listener)

    when:
    servlet.unbindServletListener(listener)
    then:
    !servlet.configuration.listeners.contains(listener)
  }

  def "context builder is bound and unbound"() {
    setup:
    def servlet = new OsgiGraphQLHttpServlet()
    def context = Mock(GraphQLKickstartContext)
    context.getDataLoaderRegistry() >> new DataLoaderRegistry()
    context.getMapOfContext() >> new HashMap<Object, Object>()
    def contextBuilder = Mock(GraphQLServletContextBuilder)
    contextBuilder.build() >> context
    def request = GraphQLRequest.createIntrospectionRequest()

    when:
    servlet.setContextBuilder(contextBuilder)
    then:
    def invocationInput = servlet.configuration.invocationInputFactory.create(request)
    invocationInput.executionInput.context == context

    when:
    servlet.unsetContextBuilder(contextBuilder)
    then:
    servlet.configuration.invocationInputFactory.create(request).executionInput.context instanceof DefaultGraphQLContext
  }

  def "root object builder is bound and unbound"() {
    setup:
    def servlet = new OsgiGraphQLHttpServlet()
    def rootObject = Mock(Object)
    def rootObjectBuilder = Mock(GraphQLServletRootObjectBuilder)
    rootObjectBuilder.build() >> rootObject
    def request = GraphQLRequest.createIntrospectionRequest()

    when:
    servlet.setRootObjectBuilder(rootObjectBuilder)
    then:
    def invocationInput = servlet.configuration.invocationInputFactory.create(request)
    invocationInput.executionInput.root == rootObject

    when:
    servlet.unsetRootObjectBuilder(rootObjectBuilder)
    then:
    servlet.configuration.invocationInputFactory.create(request).executionInput.root != rootObject
  }

  def "execution strategy is bound and unbound"() {
    setup:
    def servlet = new OsgiGraphQLHttpServlet()
    def executionStrategy = Mock(ExecutionStrategyProvider)
    def request = GraphQLRequest.createIntrospectionRequest()

    when:
    servlet.setExecutionStrategyProvider(executionStrategy)
    def invocationInput = servlet.configuration.invocationInputFactory.create(request)
    servlet.configuration.graphQLInvoker.query(invocationInput)

    then:
    1 * executionStrategy.getQueryExecutionStrategy()

    when:
    servlet.unsetExecutionStrategyProvider(executionStrategy)
    def invocationInput2 = servlet.configuration.invocationInputFactory.create(request)
    servlet.configuration.graphQLInvoker.query(invocationInput2)

    then:
    0 * executionStrategy.getQueryExecutionStrategy()
  }

  def "instrumentation provider is bound and unbound"() {
    setup:
    def servlet = new OsgiGraphQLHttpServlet()
    def instrumentation = new SimplePerformantInstrumentation()
    def instrumentationProvider = Mock(InstrumentationProvider)
    instrumentationProvider.getInstrumentation() >> instrumentation
    def request = GraphQLRequest.createIntrospectionRequest()
    instrumentation.createState(_ as InstrumentationCreateStateParameters) >> Mock(InstrumentationState)

    when:
    servlet.setInstrumentationProvider(instrumentationProvider)
    def invocationInput = servlet.configuration.invocationInputFactory.create(request)
    servlet.configuration.graphQLInvoker.query(invocationInput)

    then:
    noExceptionThrown()

    when:
    servlet.unsetInstrumentationProvider(instrumentationProvider)
    then:
    noExceptionThrown()
  }
}
