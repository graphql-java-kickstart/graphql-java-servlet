package graphql.kickstart.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.ExecutionInput
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions
import graphql.kickstart.execution.context.ContextSetting
import graphql.kickstart.execution.context.DefaultGraphQLContext
import graphql.kickstart.execution.context.GraphQLKickstartContext
import graphql.kickstart.execution.instrumentation.ConfigurableDispatchInstrumentation
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.dataloader.BatchLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderRegistry
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared
import spock.lang.Specification

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.websocket.Session
import jakarta.websocket.server.HandshakeRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger

class DataLoaderDispatchingSpec extends Specification {

  public static final int STATUS_OK = 200
  public static final String CONTENT_TYPE_JSON_UTF8 = 'application/json;charset=UTF-8'

  @Shared
  ObjectMapper mapper = new ObjectMapper()

  AbstractGraphQLHttpServlet servlet
  MockHttpServletRequest request
  MockHttpServletResponse response
  AtomicInteger fetchCounterA = new AtomicInteger()
  AtomicInteger loadCounterA = new AtomicInteger()
  AtomicInteger fetchCounterB = new AtomicInteger()
  AtomicInteger loadCounterB = new AtomicInteger()
  AtomicInteger fetchCounterC = new AtomicInteger()
  AtomicInteger loadCounterC = new AtomicInteger()

  BatchLoader<String, String> batchLoaderWithCounter(AtomicInteger fetchCounter) {
    return new BatchLoader<String, String>() {
      @Override
      CompletionStage<List<String>> load(List<String> keys) {
        fetchCounter.incrementAndGet()
        CompletableFuture.completedFuture(keys)
      }
    }
  }

  def registry() {
    DataLoaderRegistry registry = new DataLoaderRegistry()
    registry.register("A", DataLoaderFactory.newDataLoader(batchLoaderWithCounter(fetchCounterA)))
    registry.register("B", DataLoaderFactory.newDataLoader(batchLoaderWithCounter(fetchCounterB)))
    registry.register("C", DataLoaderFactory.newDataLoader(batchLoaderWithCounter(fetchCounterC)))
    registry
  }

  def setup() {
    request = new MockHttpServletRequest()
    request.setAsyncSupported(true)
    request.asyncSupported = true
    response = new MockHttpServletResponse()
  }

  def queryDataFetcher(String dataLoaderName, AtomicInteger loadCounter) {
    return new DataFetcher() {
      @Override
      Object get(DataFetchingEnvironment environment) {
        String id = environment.arguments.arg
        loadCounter.incrementAndGet()
        environment.getDataLoader(dataLoaderName).load(id)
      }
    }
  }

  def contextBuilder() {
    return new GraphQLServletContextBuilder() {
      @Override
      GraphQLKickstartContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        new DefaultGraphQLContext(registry())
      }

      @Override
      GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
        new DefaultGraphQLContext(registry())
      }

      @Override
      GraphQLKickstartContext build() {
        new DefaultGraphQLContext(registry())
      }
    }
  }

  def configureServlet(ContextSetting contextSetting) {
    servlet = TestUtils.createDataLoadingServlet(queryDataFetcher("A", loadCounterA),
        queryDataFetcher("B", loadCounterB), queryDataFetcher("C", loadCounterC)
        , contextSetting,
        contextBuilder())
  }

  def resetCounters() {
    fetchCounterA.set(0)
    fetchCounterB.set(0)
    loadCounterA.set(0)
    loadCounterB.set(0)
  }

  List<Map<String, Object>> getBatchedResponseContent() {
    mapper.readValue(response.getContentAsByteArray(), List)
  }

  Instrumentation simpleInstrumentation = new SimplePerformantInstrumentation()
  ChainedInstrumentation chainedInstrumentation = new ChainedInstrumentation(Collections.singletonList(simpleInstrumentation))
  def simpleSupplier = { simpleInstrumentation }
  def chainedSupplier = { chainedInstrumentation }

  def "batched query with per query context does not batch loads together"() {
    setup:
    configureServlet(ContextSetting.PER_QUERY_WITH_INSTRUMENTATION)
    request.addParameter('query', '[{ "query": "query { query(arg:\\"test\\") { echo(arg:\\"test\\") { echo(arg:\\"test\\") } }}" }, { "query": "query{query(arg:\\"test\\") { echo (arg:\\"test\\") { echo(arg:\\"test\\")} }}" },' +
        ' { "query": "query{queryTwo(arg:\\"test\\") { echo (arg:\\"test\\")}}" }, { "query": "query{queryTwo(arg:\\"test\\") { echo (arg:\\"test\\")}}" }]')
    resetCounters()
    request.setMethod("GET")

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.query.echo.echo == "test"
    getBatchedResponseContent()[1].data.query.echo.echo == "test"
    getBatchedResponseContent()[2].data.queryTwo.echo == "test"
    getBatchedResponseContent()[3].data.queryTwo.echo == "test"
    fetchCounterA.get() == 2
    loadCounterA.get() == 2
    fetchCounterB.get() == 2
    loadCounterB.get() == 2
    fetchCounterC.get() == 2
    loadCounterC.get() == 2
  }

  def "batched query with per request context batches all queries within the request"() {
    setup:
    servlet = configureServlet(ContextSetting.PER_REQUEST_WITH_INSTRUMENTATION)
    request.addParameter('query', '[{ "query": "query { query(arg:\\"test\\") { echo(arg:\\"test\\") { echo(arg:\\"test\\") } }}" }, { "query": "query{query(arg:\\"test\\") { echo (arg:\\"test\\") { echo(arg:\\"test\\")} }}" },' +
        ' { "query": "query{queryTwo(arg:\\"test\\") { echo (arg:\\"test\\")}}" }, { "query": "query{queryTwo(arg:\\"test\\") { echo (arg:\\"test\\")}}" }]')
    resetCounters()
    request.setMethod("GET")

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.query.echo.echo == "test"
    getBatchedResponseContent()[1].data.query.echo.echo == "test"
    getBatchedResponseContent()[2].data.queryTwo.echo == "test"
    getBatchedResponseContent()[3].data.queryTwo.echo == "test"
    fetchCounterA.get() == 1
    loadCounterA.get() == 2
    fetchCounterB.get() == 1
    loadCounterB.get() == 2
    fetchCounterC.get() == 1
    loadCounterC.get() == 2
  }

  def unwrapChainedInstrumentations(Instrumentation instrumentation) {
    if (!instrumentation instanceof ChainedInstrumentation) {
      return Collections.singletonList(instrumentation)
    } else {
      List<Instrumentation> instrumentations = new ArrayList<>()
      for (Instrumentation current : ((ChainedInstrumentation) instrumentation).getInstrumentations()) {
        if (current instanceof ChainedInstrumentation) {
          instrumentations.addAll(unwrapChainedInstrumentations(current))
        } else {
          instrumentations.add(current)
        }
      }
      return instrumentations
    }
  }

  def "PER_QUERY_WITHOUT_INSTRUMENTATION does not add instrumentation"() {
    when:
    def chainedFromContext = ContextSetting.PER_QUERY_WITHOUT_INSTRUMENTATION
        .configureInstrumentationForContext(chainedSupplier, Collections.emptyList(), DataLoaderDispatcherInstrumentationOptions.newOptions())
    def simpleFromContext = ContextSetting.PER_QUERY_WITHOUT_INSTRUMENTATION
        .configureInstrumentationForContext(simpleSupplier, Collections.emptyList(), DataLoaderDispatcherInstrumentationOptions.newOptions())
    then:
    simpleInstrumentation == simpleFromContext.get()
    chainedInstrumentation == chainedFromContext.get()
  }

  def "PER_REQUEST_WITHOUT_INSTRUMENTATION does not add instrumentation"() {
    when:
    def chainedFromContext = ContextSetting.PER_REQUEST_WITHOUT_INSTRUMENTATION
        .configureInstrumentationForContext(chainedSupplier, Collections.emptyList(), DataLoaderDispatcherInstrumentationOptions.newOptions())
    def simpleFromContext = ContextSetting.PER_REQUEST_WITHOUT_INSTRUMENTATION
        .configureInstrumentationForContext(simpleSupplier, Collections.emptyList(), DataLoaderDispatcherInstrumentationOptions.newOptions())
    then:
    simpleInstrumentation == simpleFromContext.get()
    chainedInstrumentation == chainedFromContext.get()
  }

  def "PER_QUERY_WITH_INSTRUMENTATION adds instrumentation"() {
    when:
    def chainedFromContext = ContextSetting.PER_QUERY_WITH_INSTRUMENTATION
        .configureInstrumentationForContext(chainedSupplier, Collections.emptyList(), DataLoaderDispatcherInstrumentationOptions.newOptions())
    def simpleFromContext = ContextSetting.PER_QUERY_WITH_INSTRUMENTATION
        .configureInstrumentationForContext(simpleSupplier, Collections.emptyList(), DataLoaderDispatcherInstrumentationOptions.newOptions())
    def fromSimple = unwrapChainedInstrumentations(simpleFromContext.get())
    def fromChained = unwrapChainedInstrumentations(chainedFromContext.get())
    then:
    fromSimple.size() == 2
    fromSimple.contains(simpleInstrumentation)
    fromSimple.stream().anyMatch({ inst -> inst instanceof ConfigurableDispatchInstrumentation })
    fromChained.size() == 2
    fromChained.contains(simpleInstrumentation)
    fromChained.stream().anyMatch({ inst -> inst instanceof ConfigurableDispatchInstrumentation })
  }

  def "PER_REQUEST_WITH_INSTRUMENTATION adds instrumentation"() {
    setup:
    ExecutionInput mockInput = ExecutionInput.newExecutionInput().query("query { query(arg:\"test\")").dataLoaderRegistry(new DataLoaderRegistry()).build()
    when:
    def chainedFromContext = ContextSetting.PER_REQUEST_WITH_INSTRUMENTATION
        .configureInstrumentationForContext(chainedSupplier, Collections.singletonList(mockInput), DataLoaderDispatcherInstrumentationOptions.newOptions())
    def simpleFromContext = ContextSetting.PER_REQUEST_WITH_INSTRUMENTATION
        .configureInstrumentationForContext(simpleSupplier, Collections.singletonList(mockInput), DataLoaderDispatcherInstrumentationOptions.newOptions())
    def fromSimple = unwrapChainedInstrumentations(simpleFromContext.get())
    def fromChained = unwrapChainedInstrumentations(chainedFromContext.get())
    then:
    fromSimple.size() == 2
    fromSimple.contains(simpleInstrumentation)
    fromSimple.stream().anyMatch({ inst -> inst instanceof ConfigurableDispatchInstrumentation })
    fromChained.size() == 2
    fromChained.contains(simpleInstrumentation)
    fromChained.stream().anyMatch({ inst -> inst instanceof ConfigurableDispatchInstrumentation })
  }
}
