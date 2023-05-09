package graphql.kickstart.servlet

import spock.lang.Specification

class GraphQLWebsocketServletSpec extends Specification {

  def "checkOrigin without any allowed origins allows given origin"() {
    given: "a websocket servlet with no allowed origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).build())

    when: "we check origin http://localhost:8080"
    def allowed = servlet.checkOrigin("http://localhost:8080")

    then:
    allowed
  }

  def "checkOrigin without any allowed origins allows when no origin given"() {
    given: "a websocket servlet with no allowed origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).build())

    when: "we check origin null"
    def allowed = servlet.checkOrigin(null)

    then:
    allowed
  }

  def "checkOrigin without any allowed origins allows when origin is empty"() {
    given: "a websocket servlet with no allowed origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).build())

    when: "we check origin null"
    def allowed = servlet.checkOrigin(" ")

    then:
    allowed
  }

  def "checkOrigin with allow all origins allows given origin"() {
    given: "a websocket servlet with allow all origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).allowedOrigins(List.of("*")).build())

    when: "we check origin http://localhost:8080"
    def allowed = servlet.checkOrigin("http://localhost:8080")

    then:
    allowed
  }

  def "checkOrigin with specific allowed origins allows given origin"() {
    given: "a websocket servlet with allow all origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).allowedOrigins(List.of("http://localhost:8080")).build())

    when: "we check origin http://localhost:8080"
    def allowed = servlet.checkOrigin("http://localhost:8080")

    then:
    allowed
  }

  def "checkOrigin with specific allowed origins allows given origin with trailing slash"() {
    given: "a websocket servlet with allow all origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).allowedOrigins(List.of("http://localhost:8080")).build())

    when: "we check origin http://localhost:8080/"
    def allowed = servlet.checkOrigin("http://localhost:8080/")

    then:
    allowed
  }

  def "checkOrigin with specific allowed origins with trailing slash allows given origin without trailing slash"() {
    given: "a websocket servlet with allow all origins"
    def servlet = new GraphQLWebsocketServlet(GraphQLConfiguration.with(TestUtils.createGraphQlSchema()).allowedOrigins(List.of("http://localhost:8080/")).build())

    when: "we check origin http://localhost:8080"
    def allowed = servlet.checkOrigin("http://localhost:8080")

    then:
    allowed
  }
}
