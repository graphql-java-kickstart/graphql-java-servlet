package graphql.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.ExecutionResultImpl
import graphql.kickstart.execution.GraphQLObjectMapper
import org.codehaus.groovy.runtime.StringBufferWriter
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SingleQueryResponseWriterTest extends Specification {

  @Unroll
  def "should write utf8 results into the response with content #result"() {
    given:
      def graphQLObjectMapperMock = GraphQLObjectMapper.newBuilder().withObjectMapperProvider({ new ObjectMapper() }).build()
      graphQLObjectMapperMock.getJacksonMapper() >> new ObjectMapper()

      def requestMock = Mock(HttpServletRequest)
      def responseMock = Mock(HttpServletResponse)

      def responseContentBuffer = new StringBuffer()
      responseMock.getWriter() >> new PrintWriter(new StringBufferWriter(responseContentBuffer))
      1 * responseMock.setContentLength(expectedContentLenght)
      1 * responseMock.setCharacterEncoding("UTF-8")

    expect:
      def writer = new SingleQueryResponseWriter(new ExecutionResultImpl(result, []), graphQLObjectMapperMock)
      writer.write(requestMock, responseMock)

      responseContentBuffer.toString() == expectedResponseContent

    where:
      result                || expectedContentLenght | expectedResponseContent
      [testValue: "abcde"]  || 30                    | """{"data":{"testValue":"abcde"}}"""
      [testValue: "äöüüöß"] || 37                    | """{"data":{"testValue":"äöüüöß"}}"""
  }
}
