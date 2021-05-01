package graphql.kickstart.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.kickstart.servlet.core.GraphQLServletListener
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class RequestTester {

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

  RequestTester(GraphQLServletListener... listeners) {
    subscriptionLatch = new CountDownLatch(1)
    servlet = TestUtils.createDefaultServlet(
        { env -> env.arguments.arg },
        { env -> env.arguments.arg },
        { env ->
          AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>()
          publisherRef.set(new SingleSubscriberPublisher<String>({
            SingleSubscriberPublisher<String> publisher = publisherRef.get()
            publisher.offer("First\n\n" + env.arguments.arg)
            publisher.offer("Second\n\n" + env.arguments.arg)
            publisher.noMoreData()
            subscriptionLatch.countDown()
          }))
          return publisherRef.get()
        },
        listeners)

    request = new MockHttpServletRequest()
    request.asyncSupported = true
    request.method = "GET"
    response = new MockHttpServletResponse()
  }

  Map<String, Object> getResponseContent() {
    mapper.readValue(response.getContentAsByteArray(), Map)
  }

  def addParameter(String name, String value) {
    request.addParameter(name, value)
  }

  def doGet() {
    servlet.doGet(request, response)
  }

  def assertThatResponseIsOk() {
    return response.getStatus() == STATUS_OK
  }

  def assertThatContentTypeIsJson() {
    return response.getContentType() == CONTENT_TYPE_JSON_UTF8
  }
}
