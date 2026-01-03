package graphql.kickstart.servlet

import graphql.incremental.IncrementalExecutionResult
import graphql.kickstart.execution.GraphQLObjectMapper
import jakarta.servlet.ServletOutputStream
import org.springframework.mock.web.MockAsyncContext
import spock.lang.Specification

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SingleIncrementalQueryResponseWriterTest extends Specification {

  def "result hasNext should complete"() {
    given:
    def result = Mock(IncrementalExecutionResult)
    result.hasNext() >> false
    def objectMapper = Mock(GraphQLObjectMapper)
    def writer = new SingleIncrementalQueryResponseWriter(result, objectMapper, 100)
    def request = Mock(HttpServletRequest)
    def responseOutputStream = Mock(ServletOutputStream)
    def response = Mock(HttpServletResponse)
    response.getOutputStream() >> responseOutputStream
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
