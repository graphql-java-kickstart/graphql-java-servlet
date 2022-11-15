package graphql.kickstart.servlet

import graphql.ExecutionResult
import graphql.kickstart.execution.GraphQLObjectMapper
import org.springframework.mock.web.MockAsyncContext
import spock.lang.Specification

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SingleAsynchronousQueryResponseWriterTest extends Specification {

  def "result data is no publisher should"() {
    given:
    def result = Mock(ExecutionResult)
    def objectMapper = Mock(GraphQLObjectMapper)
    def writer = new SingleAsynchronousQueryResponseWriter(result, objectMapper, 100)
    def request = Mock(HttpServletRequest)
    def responseWriter = new PrintWriter(new StringWriter())
    def response = Mock(HttpServletResponse)
    response.getWriter() >> responseWriter
    def asyncContext = new MockAsyncContext(request, response)
    request.getAsyncContext() >> asyncContext
    request.isAsyncStarted() >> true
    objectMapper.serializeResultAsJson(result) >> "{ }"

    when:
    writer.write(request, response)

    then:
    noExceptionThrown()
  }

}
