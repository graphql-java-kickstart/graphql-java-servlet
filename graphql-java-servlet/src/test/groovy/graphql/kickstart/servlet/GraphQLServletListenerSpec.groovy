package graphql.kickstart.servlet

import graphql.kickstart.servlet.core.GraphQLServletListener
import spock.lang.Specification

class GraphQLServletListenerSpec extends Specification {

  def listener = Mock(GraphQLServletListener)
  def requestCallback = Mock(GraphQLServletListener.RequestCallback)
  def tester = new RequestTester(listener)

  def "query over HTTP GET calls onRequest listener"() {
    given: "a valid graphql query request"
    tester.addParameter('query', 'query { echo(arg:"test") }')

    and: "a listener that always returns request callback"
    listener.onRequest(tester.request, tester.response) >> requestCallback

    when: "we execute a GET request"
    tester.doGet()

    then:
    tester.assertThatResponseIsOk()
    tester.assertThatContentTypeIsJson()
    1 * listener.onRequest(tester.request, tester.response)
  }

  def "query over HTTP GET calls onSuccess callback"() {
    given: "a valid graphql query request"
    tester.addParameter('query', 'query { echo(arg:"test") }')

    and: "a listener that always returns request callback"
    listener.onRequest(tester.request, tester.response) >> requestCallback

    when: "we execute a GET request"
    tester.doGet()

    then:
    tester.assertThatResponseIsOk()
    tester.assertThatContentTypeIsJson()
    1 * requestCallback.onSuccess(tester.request, tester.response)
  }

}
