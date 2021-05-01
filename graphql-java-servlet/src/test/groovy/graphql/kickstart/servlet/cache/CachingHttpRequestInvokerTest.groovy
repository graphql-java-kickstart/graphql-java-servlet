package graphql.kickstart.servlet.cache

import graphql.kickstart.execution.GraphQLInvoker
import graphql.kickstart.execution.GraphQLQueryResult
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput
import graphql.kickstart.servlet.GraphQLConfiguration
import graphql.kickstart.servlet.HttpRequestInvoker
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.CompletableFuture

class CachingHttpRequestInvokerTest extends Specification {

  def cacheReaderMock
  def cachingInvoker
  def invocationInputMock
  def requestMock
  def responseMock
  def responseCacheManagerMock
  def httpRequestInvokerMock
  def graphqlInvoker
  def configuration

  def setup() {
    cacheReaderMock = Mock(CacheReader)
    invocationInputMock = Mock(GraphQLSingleInvocationInput)
    requestMock = Mock(HttpServletRequest)
    responseMock = Mock(HttpServletResponse)
    responseCacheManagerMock = Mock(GraphQLResponseCacheManager)
    configuration = Mock(GraphQLConfiguration)
    httpRequestInvokerMock = Mock(HttpRequestInvoker)
    graphqlInvoker = Mock(GraphQLInvoker)
    cachingInvoker = new CachingHttpRequestInvoker(configuration, httpRequestInvokerMock, cacheReaderMock)

    configuration.getResponseCacheManager() >> responseCacheManagerMock
    configuration.getGraphQLInvoker() >> graphqlInvoker
    graphqlInvoker.queryAsync(invocationInputMock) >> CompletableFuture.completedFuture(Mock(GraphQLQueryResult))
  }

  def "should execute regular invoker if cache not exists"() {
    given:
    cacheReaderMock.responseFromCache(invocationInputMock, requestMock, responseMock, responseCacheManagerMock) >> false

    when:
    cachingInvoker.execute(invocationInputMock, requestMock, responseMock)

    then:
    1 * httpRequestInvokerMock.execute(invocationInputMock, requestMock, responseMock)
  }

  def "should not execute regular invoker if cache exists"() {
    given:
    cacheReaderMock.responseFromCache(invocationInputMock, requestMock, responseMock, responseCacheManagerMock) >> true

    when:
    cachingInvoker.execute(invocationInputMock, requestMock, responseMock)

    then:
    0 * httpRequestInvokerMock.execute(invocationInputMock, requestMock, responseMock)
  }

  def "should return bad request response when ioexception"() {
    given:
    cacheReaderMock.responseFromCache(invocationInputMock, requestMock, responseMock, responseCacheManagerMock) >> { throw new IOException() }

    when:
    cachingInvoker.execute(invocationInputMock, requestMock, responseMock)

    then:
    1 * responseMock.setStatus(400)
  }

  def "should initialize completely when using single param constructor"() {
    given:
    def invoker = new CachingHttpRequestInvoker(configuration)

    when:
    invoker.execute(invocationInputMock, requestMock, responseMock)

    then:
    noExceptionThrown()
  }

}
