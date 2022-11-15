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

class BatchedQueryResponseWriterTest extends Specification {

  @Unroll
  def "should write utf8 results into the response with content #result"() {
    given:
    def byteArrayOutputStream = new ByteArrayOutputStream()
    def graphQLObjectMapperMock = GraphQLObjectMapper.newBuilder().withObjectMapperProvider({ new ObjectMapper() }).build()
    graphQLObjectMapperMock.getJacksonMapper() >> new ObjectMapper()

    def requestMock = Mock(HttpServletRequest)
    def responseMock = Mock(HttpServletResponse)
    def servletOutputStreamMock = Mock(ServletOutputStream)

    responseMock.getOutputStream() >> servletOutputStreamMock

    1 * responseMock.setContentLength(expectedContentLengh)
    1 * responseMock.setCharacterEncoding(StandardCharsets.UTF_8.name())
    (1.._) * servletOutputStreamMock.write(_) >> { value ->
      byteArrayOutputStream.write((byte[]) (value[0]))
    }

    def executionResultList = new ArrayList()
    for (LinkedHashMap<Object, Object> value : result) {
      executionResultList.add(new ExecutionResultImpl(value, []))
    }

    def writer = new BatchedQueryResponseWriter(executionResultList, graphQLObjectMapperMock)

    when:
    writer.write(requestMock, responseMock)

    then:
    byteArrayOutputStream.toString(StandardCharsets.UTF_8.name()) == expectedResponseContent

    where:
    result                      || expectedContentLengh | expectedResponseContent
    [[testValue: "abcde"]]      || 32                   | """[{"data":{"testValue":"abcde"}}]"""
    [[testValue: "äöüüöß"]]     || 39                   | """[{"data":{"testValue":"äöüüöß"}}]"""
    []                          || 2                    | """[]"""
    [[k1: "äöüüöß"], [k2: "a"]] || 52                   | """[{"data":{"k1":"äöüüöß"}},{"data":{"k2":"a"}}]"""
  }
}
