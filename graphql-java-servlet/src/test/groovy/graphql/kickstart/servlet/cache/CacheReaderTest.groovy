package graphql.kickstart.servlet.cache

import graphql.kickstart.execution.input.GraphQLInvocationInput
import spock.lang.Specification

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class CacheReaderTest extends Specification {

  def cacheManager
  def invocationInput
  def request
  def response
  def cacheReader
  def cachedResponse

  def setup() {
    cacheManager = Mock(GraphQLResponseCacheManager)
    invocationInput = Mock(GraphQLInvocationInput)
    request = Mock(HttpServletRequest)
    response = Mock(HttpServletResponse)
    cacheReader = new CacheReader()
    cachedResponse = Mock(CachedResponse)
  }

  def "should return false if no cached response"() {
    given:
    cacheManager.get(request, invocationInput) >> null

    when:
    def result = cacheReader.responseFromCache(invocationInput, request, response, cacheManager)

    then:
    !result
  }

  def "should send error response if cached response is error"() {
    given:
    cachedResponse.isError() >> true
    cachedResponse.getErrorStatusCode() >> 10
    cachedResponse.getErrorMessage() >> "some error"
    cacheManager.get(request, invocationInput) >> cachedResponse

    when:
    def result = cacheReader.responseFromCache(invocationInput, request, response, cacheManager)

    then:
    result
    1 * response.sendError(10, "some error")
  }

  def "should send success response if cached response is ok"() {
    given:
    def outputStream = Mock(ServletOutputStream)
    cachedResponse.isError() >> false
    cachedResponse.getContentBytes() >> [00, 01, 02]
    response.getOutputStream() >> outputStream
    cacheManager.get(request, invocationInput) >> cachedResponse

    when:
    def result = cacheReader.responseFromCache(invocationInput, request, response, cacheManager)

    then:
    result
    1 * response.setContentType("application/json;charset=UTF-8")
    1 * response.setStatus(200)
    1 * response.setCharacterEncoding("UTF-8")
    1 * response.setContentLength(3)
    1 * outputStream.write([00, 01, 02])
  }

  def "should return false if exception is thrown"() {
    given:
    cacheManager.get(request, invocationInput) >> { throw new RuntimeException() }

    when:
    def result = cacheReader.responseFromCache(invocationInput, request, response, cacheManager)

    then:
    !result
  }
}
