package graphql.kickstart.servlet.cache

import graphql.kickstart.execution.input.GraphQLInvocationInput
import graphql.kickstart.servlet.GraphQLConfiguration
import graphql.kickstart.servlet.HttpRequestInvoker
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CachingHttpRequestInvokerTest extends Specification {

    def cacheReaderMock
    def cachingInvoker
    def invocationInputMock
    def requestMock
    def responseMock
    def responseCacheManagerMock
    def httpRequestInvokerMock

    def setup() {
        cacheReaderMock = Mock(CacheReader)
        invocationInputMock = Mock(GraphQLInvocationInput)
        requestMock = Mock(HttpServletRequest)
        responseMock = Mock(HttpServletResponse)
        responseCacheManagerMock = Mock(GraphQLResponseCacheManager)
        def configuration = Mock(GraphQLConfiguration)
        httpRequestInvokerMock = Mock(HttpRequestInvoker)
        cachingInvoker = new CachingHttpRequestInvoker(configuration, httpRequestInvokerMock, cacheReaderMock)

        configuration.getResponseCacheManager() >> responseCacheManagerMock
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

}
