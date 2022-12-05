package graphql.kickstart.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.ExecutionResultImpl
import graphql.kickstart.execution.GraphQLObjectMapper
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets

class SingleQueryResponseWriterTest extends Specification {

  @Unroll
  def "should write utf8 results into the response with content #result"() {
    given:
    def graphQLObjectMapperMock = GraphQLObjectMapper.newBuilder().withObjectMapperProvider({ new ObjectMapper() }).build()
    graphQLObjectMapperMock.getJacksonMapper() >> new ObjectMapper()

    def requestMock = Mock(HttpServletRequest)
    def responseMock = Mock(HttpServletResponse)
    responseMock.getOutputStream() >> Mock(ServletOutputStream)

    1 * responseMock.setContentLength(expectedContentLenght)
    1 * responseMock.setCharacterEncoding(StandardCharsets.UTF_8.name())
    1 * responseMock.getOutputStream().write(expectedResponseContent.getBytes(StandardCharsets.UTF_8))

    expect:
    def writer = new SingleQueryResponseWriter(new ExecutionResultImpl(result, []), graphQLObjectMapperMock)
    writer.write(requestMock, responseMock)

    where:
    result                || expectedContentLenght | expectedResponseContent
    [testValue: "abcde"]  || 30                    | """{"data":{"testValue":"abcde"}}"""
    [testValue: "äöüüöß"] || 37                    | """{"data":{"testValue":"äöüüöß"}}"""
  }
}
