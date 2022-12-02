package graphql.kickstart.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.Scalars
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory
import graphql.language.Field
import graphql.schema.GraphQLNonNull
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Andrew Potter
 */
class AbstractGraphQLHttpServletSpec extends Specification {

  public static final int STATUS_OK = 200
  public static final int STATUS_BAD_REQUEST = 400
  public static final int STATUS_ERROR = 500
  public static final String CONTENT_TYPE_JSON_UTF8 = 'application/json;charset=UTF-8'
  public static final String CONTENT_TYPE_SERVER_SENT_EVENTS = 'text/event-stream;charset=UTF-8'

  @Shared
  ObjectMapper mapper = new ObjectMapper()

  AbstractGraphQLHttpServlet servlet
  MockHttpServletRequest request
  MockHttpServletResponse response
  CountDownLatch subscriptionLatch

  def setup() {
    subscriptionLatch = new CountDownLatch(1)
    servlet = TestUtils.createDefaultServlet({ env -> env.arguments.arg }, { env -> env.arguments.arg }, { env ->
      AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
      publisherRef.set(new SingleSubscriberPublisher<String>({
        SingleSubscriberPublisher<String> publisher = publisherRef.get()
        publisher.offer("First\n\n" + env.arguments.arg)
        publisher.offer("Second\n\n" + env.arguments.arg)
        publisher.noMoreData()
        subscriptionLatch.countDown()
      }))
      return publisherRef.get()
    })

    request = new MockHttpServletRequest()
    request.setAsyncSupported(true)
    request.asyncSupported = true
    request.setMethod("GET")
    response = new MockHttpServletResponse()
  }


  Map<String, Object> getResponseContent() {
    mapper.readValue(response.getContentAsByteArray(), Map)
  }

  List<Map<String, Object>> getSubscriptionResponseContent() {
    String[] data = response.getContentAsString().split("\n\n")
    return data.collect { dataLine ->
      if (dataLine.startsWith("data: ")) {
        return mapper.readValue(dataLine.substring(5), Map)
      } else {
        throw new IllegalStateException("Could not read event stream")
      }
    }
  }

  List<Map<String, Object>> getBatchedResponseContent() {
    mapper.readValue(response.getContentAsByteArray(), List)
  }

  def "HTTP GET without info returns bad request"() {
    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_BAD_REQUEST
  }

  def "HTTP GET to /schema.json returns introspection query"() {
    setup:
    request.setPathInfo('/schema.json')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.__schema != null
  }

  def "query over HTTP GET returns data"() {
    setup:
    request.addParameter('query', 'query { echo(arg:"test") }')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    response.getContentLength() == mapper.writeValueAsString(["data": ["echo": "test"]]).getBytes(StandardCharsets.UTF_8).length
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP GET returns data with correct contentLength"() {
    setup:
    request.addParameter('query', 'query { echo(arg:"special char á") }')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    response.getContentLength() == mapper.writeValueAsString(["data": ["echo": "special char á"]]).getBytes(StandardCharsets.UTF_8).length
    getResponseContent().data.echo == "special char á"
  }

  def "disabling async support on request over HTTP GET does not start async request"() {
    setup:
    servlet = TestUtils.createDefaultServlet({ env -> env.arguments.arg }, { env -> env.arguments.arg }, { env ->
      AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
      publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
        publisherRef.get().offer((String) env.arguments.arg)
        publisherRef.get().noMoreData()
      }))
      return publisherRef.get()
    })
    request.addParameter('query', 'query { echo(arg:"test") }')
    request.setAsyncSupported(false)

    when:
    servlet.doGet(request, response)

    then:
    request.asyncContext == null
  }

  def "query over HTTP GET with variables returns data"() {
    setup:
    request.addParameter('query', 'query Echo($arg: String) { echo(arg:$arg) }')
    request.addParameter('variables', '{"arg": "test"}')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP GET with variables as string returns data"() {
    setup:
    request.addParameter('query', 'query Echo($arg: String) { echo(arg:$arg) }')
    request.addParameter('variables', '"{\\"arg\\": \\"test\\"}"')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP GET with operationName returns data"() {
    when:
    response = new MockHttpServletResponse()
    request.addParameter('query', 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }')
    request.addParameter('operationName', 'two')
    servlet.doGet(request, response)
    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echoOne == null
    getResponseContent().data.echoTwo == "test-two"

  }

  def "query over HTTP GET with empty non-null operationName returns data"() {
    when:
    response = new MockHttpServletResponse()
    request.addParameter('query', 'query echo{ echo: echo(arg:"test") }')
    request.addParameter('operationName', '')
    servlet.doGet(request, response)
    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP GET with unknown property 'test' returns data"() {
    setup:
    request.addParameter('query', 'query { echo(arg:"test") }')
    request.addParameter('test', 'test')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "batched query over HTTP GET returns data"() {
    setup:
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP GET returns data with correct contentLength"() {
    setup:
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"special char á\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    response.getContentLength() == mapper.writeValueAsString([["data": ["echo": "special char á"]], ["data": ["echo": "test"]]]).getBytes(StandardCharsets.UTF_8).length
    getBatchedResponseContent()[0].data.echo == "special char á"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP GET with variables returns data"() {
    setup:
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }, { "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP GET with variables as string returns data"() {
    setup:
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }", "variables": "{ \\"arg\\": \\"test\\" }" }, { "query": "query { echo(arg:\\"test\\") }", "variables": "{ \\"arg\\": \\"test\\" }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP GET with operationName returns data"() {
    when:
    response = new MockHttpServletResponse()
    request.addParameter('query', '[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]')
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echoOne == "test-one"
    getBatchedResponseContent()[0].data.echoTwo == null
    getBatchedResponseContent()[1].data.echoOne == null
    getBatchedResponseContent()[1].data.echoTwo == "test-two"
  }

  def "batched query over HTTP GET with empty non-null operationName returns data"() {
    when:
    response = new MockHttpServletResponse()
    request.addParameter('query', '[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]')
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP GET with unknown property 'test' returns data"() {
    setup:
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "Batch Execution Handler allows limiting batches and sending error messages."() {
    setup:
    servlet = TestUtils.createBatchCustomizedServlet({ env -> env.arguments.arg }, { env -> env.arguments.arg }, { env ->
      AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
      publisherRef.set(new SingleSubscriberPublisher<String>({
        SingleSubscriberPublisher<String> publisher = publisherRef.get()
        publisher.offer("First\n\n" + env.arguments.arg)
        publisher.offer("Second\n\n" + env.arguments.arg)
        publisher.noMoreData()
        subscriptionLatch.countDown()
      }))
      return publisherRef.get()
    })
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_BAD_REQUEST
    response.getErrorMessage() == TestBatchInputPreProcessor.BATCH_ERROR_MESSAGE
  }

  def "Default Execution Result Handler does not limit number of queries"() {
    setup:
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent().size() == 3
  }

  def "mutation over HTTP GET returns errors"() {
    setup:
    request.addParameter('query', 'mutation { echo(arg:"test") }')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().errors.size() == 1
  }

  def "batched mutation over HTTP GET returns errors"() {
    setup:
    request.addParameter('query', '[{ "query": "mutation { echo(arg:\\"test\\") }" }, { "query": "mutation {echo(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].errors.size() == 1
    getBatchedResponseContent()[1].errors.size() == 1
  }

  def "subscription query over HTTP GET with variables as string returns data"() {
    setup:
    request.addParameter('query', 'subscription Subscription($arg: String!) { echo(arg: $arg) }')
    request.addParameter('operationName', 'Subscription')
    request.addParameter('variables', '{"arg": "test"}')
    request.setAsyncSupported(true)

    when:
    servlet.doGet(request, response)
    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_SERVER_SENT_EVENTS

    when:
    subscriptionLatch.await(1, TimeUnit.SECONDS)
    then:
    getSubscriptionResponseContent()[0].data.echo == "First\n\ntest"
    getSubscriptionResponseContent()[1].data.echo == "Second\n\ntest"
  }

  def "query over HTTP POST without part or body returns bad request"() {
    when:
    request.setMethod("POST")
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_BAD_REQUEST
  }

  def "query over HTTP POST body returns data"() {
    setup:
    request.setContent(mapper.writeValueAsBytes([
        query: 'query { echo(arg:"test") }'
    ]))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multiline body returns data"() {
    setup:
    request.setContent("""
        query { object {
a
b
        } }""".bytes)
    request.setMethod("POST")
    request.contentType = "application/graphql"

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.object.b == null
  }

  def "disabling async support on request over HTTP POST does not start async request"() {
    setup:
    servlet = TestUtils.createDefaultServlet({ env -> env.arguments.arg }, { env -> env.arguments.arg }, { env ->
      AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
      publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
        publisherRef.get().offer((String) env.arguments.arg)
        publisherRef.get().noMoreData()
      }))
      return publisherRef.get()
    })
    request.setContent(mapper.writeValueAsBytes([
        query: 'query { echo(arg:"test") }'
    ]))
    request.setAsyncSupported(false)

    when:
    servlet.doPost(request, response)

    then:
    request.asyncContext == null
  }

  def "query over HTTP POST body with graphql contentType returns data"() {
    setup:
    request.addHeader("Content-Type", "application/graphql")
    request.setContent('query { echo(arg:"test") }'.getBytes("UTF-8"))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST body with variables returns data"() {
    setup:
    request.setContent(mapper.writeValueAsBytes([
        query    : 'query Echo($arg: String) { echo(arg:$arg) }',
        variables: '{"arg": "test"}'
    ]))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST body with operationName returns data"() {
    setup:
    request.setContent(mapper.writeValueAsBytes([
        query        : 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }',
        operationName: 'two'
    ]))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echoOne == null
    getResponseContent().data.echoTwo == "test-two"
  }

  def "query over HTTP POST body with empty non-null operationName returns data"() {
    setup:
    request.setContent(mapper.writeValueAsBytes([
        query        : 'query echo{ echo: echo(arg:"test") }',
        operationName: ''
    ]))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST body with unknown property 'test' returns data"() {
    setup:
    request.setContent(mapper.writeValueAsBytes([
        query: 'query { echo(arg:"test") }',
        test : 'test'
    ]))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'graphql' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=---test")
    request.setMethod("POST")

    request.addPart(TestMultipartContentBuilder.createPart('graphql', mapper.writeValueAsString([query: 'query { echo(arg:"test") }'])))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'query' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', 'query { echo(arg:"test") }'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'query' with operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }'))
    request.addPart(TestMultipartContentBuilder.createPart('operationName', 'two'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echoOne == null
    getResponseContent().data.echoTwo == "test-two"
  }

  def "query over HTTP POST multipart named 'query' with empty non-null operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', 'query echo{ echo: echo(arg:"test") }'))
    request.addPart(TestMultipartContentBuilder.createPart('operationName', ''))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'query' with variables returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', 'query Echo($arg: String) { echo(arg:$arg) }'))
    request.addPart(TestMultipartContentBuilder.createPart('variables', '{"arg": "test"}'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'query' with unknown property 'test' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', 'query { echo(arg:"test") }'))
    request.addPart(TestMultipartContentBuilder.createPart('test', 'test'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'operations' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{"query": "query { echo(arg:\\"test\\") }"}'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'operations' with operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{"query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two"}'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echoOne == null
    getResponseContent().data.echoTwo == "test-two"
  }

  def "query over HTTP POST multipart named 'operations' with empty non-null operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{"query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'operations' with variables returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{"query": "query Echo($arg: String) { echo(arg:$arg) }", "variables": {"arg": "test"} }'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'operations' with unknown property 'test' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{\"query\": \"query { echo(arg:\\"test\\") }\"}'))
    request.addPart(TestMultipartContentBuilder.createPart('test', 'test'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "query over HTTP POST multipart named 'operations' will interpolate variables from map"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{"query": "mutation test($file: Upload!) { echoFile(file: $file) }", "variables": { "file": null }}'))
    request.addPart(TestMultipartContentBuilder.createPart('map', '{"0": ["variables.file"]}'))
    request.addPart(TestMultipartContentBuilder.createPart('0', 'test'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echoFile == "test"
  }

  def "query over HTTP POST multipart named 'operations' will interpolate variable list from map"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '{"query": "mutation test($files: [Upload!]!) { echoFiles(files: $files) }", "variables": { "files": [null, null] }}'))
    request.addPart(TestMultipartContentBuilder.createPart('map', '{"0": ["variables.files.0"], "1": ["variables.files.1"]}'))
    request.addPart(TestMultipartContentBuilder.createPart('0', 'test'))
    request.addPart(TestMultipartContentBuilder.createPart('1', 'test again'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echoFiles == ["test", "test again"]
  }

  def "errors while accessing file from the request"() {
    setup:
    request = Spy(MockHttpServletRequest)
    request.setMethod("POST")
    request.setContentType("multipart/form-data, boundary=test")
    // See https://github.com/apache/tomcat/blob/main/java/org/apache/catalina/connector/Request.java#L2775...L2791
    request.getParts() >> { throw new IllegalStateException() }

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_BAD_REQUEST
    response.getContentLength() == 0
  }

  def "batched query over HTTP POST body returns data"() {
    setup:
    request.setContent('[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    response.getContentLength() == mapper.writeValueAsString([["data": ["echo": "test"]], ["data": ["echo": "test"]]]).length()
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST body with variables returns data"() {
    setup:
    request.setContent('[{ "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }, { "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST body with operationName returns data"() {
    setup:
    request.setContent('[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echoOne == "test-one"
    getBatchedResponseContent()[0].data.echoTwo == null
    getBatchedResponseContent()[1].data.echoOne == null
    getBatchedResponseContent()[1].data.echoTwo == "test-two"
  }

  def "batched query over HTTP POST body with empty non-null operationName returns data"() {
    setup:
    request.setContent('[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST body with unknown property 'test' returns data"() {
    setup:
    request.setContent('[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'graphql' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")

    request.addPart(TestMultipartContentBuilder.createPart('graphql', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'graphql' with unknown property 'test' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")

    request.addPart(TestMultipartContentBuilder.createPart('graphql', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'query' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'query' with operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', '[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echoOne == "test-one"
    getBatchedResponseContent()[0].data.echoTwo == null
    getBatchedResponseContent()[1].data.echoOne == null
    getBatchedResponseContent()[1].data.echoTwo == "test-two"
  }

  def "batched query over HTTP POST multipart named 'query' with empty non-null operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', '[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'query' with variables returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', '[{ "query": "query echo($arg: String) { echo(arg:$arg) }", "variables": { "arg": "test" } }, { "query": "query echo($arg: String) { echo(arg:$arg) }", "variables": { "arg": "test" } }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'query' with unknown property 'test' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('query', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'operations' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")

    request.addPart(TestMultipartContentBuilder.createPart('operations', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'operations' with unknown property 'test' returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")

    request.addPart(TestMultipartContentBuilder.createPart('operations', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'operations' with operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echoOne == "test-one"
    getBatchedResponseContent()[0].data.echoTwo == null
    getBatchedResponseContent()[1].data.echoOne == null
    getBatchedResponseContent()[1].data.echoTwo == "test-two"
  }

  def "batched query over HTTP POST multipart named 'operations' with empty non-null operationName returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched query over HTTP POST multipart named 'operations' with variables returns data"() {
    setup:
    request.setContentType("multipart/form-data, boundary=test")
    request.setMethod("POST")
    request.addPart(TestMultipartContentBuilder.createPart('operations', '[{ "query": "query echo($arg: String) { echo(arg:$arg) }", "variables": { "arg": "test" } }, { "query": "query echo($arg: String) { echo(arg:$arg) }", "variables": { "arg": "test" } }]'))

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "mutation over HTTP POST body returns data"() {
    setup:
    request.setContent(mapper.writeValueAsBytes([
        query: 'mutation { echo(arg:"test") }'
    ]))
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getResponseContent().data.echo == "test"
  }

  def "batched mutation over HTTP POST body returns data"() {
    setup:
    request.setContent('[{ "query": "mutation { echo(arg:\\"test\\") }" }, { "query": "mutation { echo(arg:\\"test\\") }" }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "batched mutation over HTTP POST body with unknown property 'test' returns data"() {
    setup:
    request.setContent('[{ "query": "mutation { echo(arg:\\"test\\") }", "test": "test" }, { "query": "mutation { echo(arg:\\"test\\") }", "test": "test" }]'.bytes)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    getBatchedResponseContent()[0].data.echo == "test"
    getBatchedResponseContent()[1].data.echo == "test"
  }

  def "subscription query over HTTP POST with variables as string returns data"() {
    setup:
    request.setContent('{"query": "subscription Subscription($arg: String!) { echo(arg: $arg) }", "operationName": "Subscription", "variables": {"arg": "test"}}'.bytes)
    request.setAsyncSupported(true)
    request.setMethod("POST")

    when:
    servlet.doPost(request, response)
    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_SERVER_SENT_EVENTS

    when:
    subscriptionLatch.await(1, TimeUnit.SECONDS)
    then:
    getSubscriptionResponseContent()[0].data.echo == "First\n\ntest"
    getSubscriptionResponseContent()[1].data.echo == "Second\n\ntest"
  }

  def "errors before graphql schema execution return internal server error"() {
    setup:
    GraphQLConfiguration configuration = GraphQLConfiguration.with(GraphQLInvocationInputFactory.newBuilder {
      throw new TestException()
    }.build()).build()
    servlet = GraphQLHttpServlet.with(configuration)
    servlet.init()

    request.setPathInfo('/schema.json')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_BAD_REQUEST
  }

  def "errors while data fetching are masked in the response"() {
    setup:
    servlet = TestUtils.createDefaultServlet({ throw new TestException() })
    request.addParameter('query', 'query { echo(arg:"test") }')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    def errors = getResponseContent().errors
    errors.size() == 1
    errors.first().message.startsWith("Internal Server Error(s)")
  }

  def "errors that also implement GraphQLError thrown while data fetching are passed to caller"() {
    setup:
    servlet = TestUtils.createDefaultServlet({ throw new TestGraphQLErrorException("This is a test message") })
    request.addParameter('query', 'query { echo(arg:"test") }')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    def errors = getResponseContent().errors
    errors.size() == 1
    errors.first().extensions.foo == "bar"
    errors.first().message.startsWith("Exception while fetching data (/echo) : This is a test message")
  }

  def "batched errors while data fetching are masked in the response"() {
    setup:
    servlet = TestUtils.createDefaultServlet({ throw new TestException() })
    request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    def errors = getBatchedResponseContent().errors
    errors[0].size() == 1
    errors[0].first().message.startsWith("Internal Server Error(s)")
    errors[1].size() == 1
    errors[1].first().message.startsWith("Internal Server Error(s)")
  }

  def "data field is present and null if no data can be returned"() {
    setup:
    request.addParameter('query', 'query { not-a-field(arg:"test") }')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    def resp = getResponseContent()
    resp.containsKey("data")
    resp.data == null
    resp.errors != null
  }

  def "batched data field is present and null if no data can be returned"() {
    setup:
    request.addParameter('query', '[{ "query": "query { not-a-field(arg:\\"test\\") }" }, { "query": "query { not-a-field(arg:\\"test\\") }" }]')

    when:
    servlet.doGet(request, response)

    then:
    response.getStatus() == STATUS_OK
    response.getContentType() == CONTENT_TYPE_JSON_UTF8
    def resp = getBatchedResponseContent()
    resp[0].containsKey("data")
    resp[0].data == null
    resp[0].errors != null
    resp[1].containsKey("data")
    resp[1].data == null
    resp[1].errors != null
  }

  def "typeInfo is serialized correctly"() {
    setup:
    MergedField field = MergedField.newMergedField().addField(new Field("test")).build()
    ExecutionStepInfo stepInfo = ExecutionStepInfo.newExecutionStepInfo().field(field).type(new GraphQLNonNull(Scalars.GraphQLString)).build()

    expect:
    servlet.getConfiguration().getObjectMapper().getJacksonMapper().writeValueAsString(stepInfo) != "{}"
  }

}
