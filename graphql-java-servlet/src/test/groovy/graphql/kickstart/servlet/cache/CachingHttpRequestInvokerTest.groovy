package graphql.kickstart.servlet.cache

import graphql.ExecutionResult
import graphql.kickstart.execution.FutureExecutionResult
import graphql.kickstart.execution.GraphQLInvoker
import graphql.kickstart.execution.GraphQLObjectMapper
import graphql.kickstart.execution.GraphQLQueryResult
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput
import graphql.kickstart.servlet.GraphQLConfiguration
import graphql.kickstart.servlet.HttpRequestInvoker
import graphql.kickstart.servlet.ListenerHandler
import spock.lang.Specification

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
  def graphqlObjectMapper
  def outputStreamMock
  def listenerHandlerMock

  def setup() {
    cacheReaderMock = Mock(CacheReader)
    invocationInputMock = Mock(GraphQLSingleInvocationInput)
    requestMock = Mock(HttpServletRequest)
    responseMock = Mock(HttpServletResponse)
    responseCacheManagerMock = Mock(GraphQLResponseCacheManager)
    configuration = Mock(GraphQLConfiguration)
    httpRequestInvokerMock = Mock(HttpRequestInvoker)
    graphqlInvoker = Mock(GraphQLInvoker)
    graphqlObjectMapper = Mock(GraphQLObjectMapper)
    outputStreamMock = Mock(ServletOutputStream)
    graphqlInvoker.execute(invocationInputMock) >> FutureExecutionResult.single(invocationInputMock, CompletableFuture.completedFuture(Mock(GraphQLQueryResult)))
    cachingInvoker = new CachingHttpRequestInvoker(configuration, httpRequestInvokerMock, cacheReaderMock)
    listenerHandlerMock = Mock(ListenerHandler)

    configuration.getResponseCacheManager() >> responseCacheManagerMock
    configuration.getGraphQLInvoker() >> graphqlInvoker
    configuration.getObjectMapper() >> graphqlObjectMapper
    graphqlObjectMapper.serializeResultAsBytes(_ as ExecutionResult) >> new byte[0]
    graphqlInvoker.queryAsync(invocationInputMock) >> CompletableFuture.completedFuture(Mock(GraphQLQueryResult))

    responseMock.getOutputStream() >> outputStreamMock
  }

  def "should execute regular invoker if cache not exists"() {
    given:
    cacheReaderMock.responseFromCache(invocationInputMock, requestMock, responseMock, responseCacheManagerMock) >> false

    when:
    cachingInvoker.execute(invocationInputMock, requestMock, responseMock, listenerHandlerMock)

    then:
    1 * httpRequestInvokerMock.execute(invocationInputMock, requestMock, responseMock, listenerHandlerMock)
  }

  def "should not execute regular invoker if cache exists"() {
    given:
    cacheReaderMock.responseFromCache(invocationInputMock, requestMock, responseMock, responseCacheManagerMock) >> true

    when:
    cachingInvoker.execute(invocationInputMock, requestMock, responseMock, listenerHandlerMock)

    then:
    0 * httpRequestInvokerMock.execute(invocationInputMock, requestMock, responseMock, listenerHandlerMock)
  }

  def "should return bad request response when ioexception"() {
    given:
    cacheReaderMock.responseFromCache(invocationInputMock, requestMock, responseMock, responseCacheManagerMock) >> { throw new IOException() }

    when:
    cachingInvoker.execute(invocationInputMock, requestMock, responseMock, listenerHandlerMock)

    then:
    1 * responseMock.setStatus(400)
  }

  def "should initialize completely when using single param constructor"() {
    given:
    def invoker = new CachingHttpRequestInvoker(configuration)

    when:
    invoker.execute(invocationInputMock, requestMock, responseMock, listenerHandlerMock)

    then:
    noExceptionThrown()
  }

}
