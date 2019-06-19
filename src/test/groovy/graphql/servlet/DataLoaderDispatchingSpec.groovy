package graphql.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.servlet.context.DefaultGraphQLServletContext
import graphql.servlet.context.GraphQLContext
import graphql.servlet.context.GraphQLContextBuilder
import graphql.servlet.context.ContextSetting
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.websocket.Session
import javax.websocket.server.HandshakeRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class DataLoaderDispatchingSpec extends Specification {

    public static final int STATUS_OK = 200
    public static final String CONTENT_TYPE_JSON_UTF8 = 'application/json;charset=UTF-8'

    @Shared
    ObjectMapper mapper = new ObjectMapper()

    AbstractGraphQLHttpServlet servlet
    MockHttpServletRequest request
    MockHttpServletResponse response
    AtomicInteger fetchCounter = new AtomicInteger()
    AtomicInteger loadCounter = new AtomicInteger()

    BatchLoader<String, String> batchLoaderA = new BatchLoader<String, String>() {
        @Override
        CompletionStage<List<String>> load(List<String> keys) {
            fetchCounter.incrementAndGet()
            CompletableFuture.completedFuture(keys)
        }
    }

    def registry() {
        DataLoaderRegistry registry = new DataLoaderRegistry()
        registry.register("A", DataLoader.newDataLoader(batchLoaderA))
        registry
    }

    def setup() {
        request = new MockHttpServletRequest()
        request.setAsyncSupported(true)
        request.asyncSupported = true
        response = new MockHttpServletResponse()
    }

    def queryDataFetcher() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) {
                String id = environment.arguments.arg
                loadCounter.incrementAndGet()
                environment.getDataLoader("A").load(id)
            }
        }
    }

    def contextBuilder () {
        return new GraphQLContextBuilder() {
            @Override
            GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
                new DefaultGraphQLServletContext(registry(), null)
            }

            @Override
            GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
                new DefaultGraphQLServletContext(registry(), null)
            }

            @Override
            GraphQLContext build() {
                new DefaultGraphQLServletContext(registry(), null)
            }
        }
    }

    def configureServlet(ContextSetting contextSetting) {
        servlet = TestUtils.createDataLoadingServlet( queryDataFetcher(),
                { env -> env.arguments.arg },
                { env ->
                    AtomicReference<SingleSubscriberPublisher<String>> publisherRef = new AtomicReference<>();
                    publisherRef.set(new SingleSubscriberPublisher<>({ subscription ->
                        publisherRef.get().offer(env.arguments.arg)
                        publisherRef.get().noMoreData()
                    }))
                    return publisherRef.get()
                },  false,  contextSetting,
                contextBuilder())
    }

    def "batched query with per query context does not batch loads together"() {
        setup:
        configureServlet(ContextSetting.PER_QUERY)
        request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')
        fetchCounter.set(0)
        loadCounter.set(0)

        when:
        servlet.doGet(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
        fetchCounter.get() == 2
        loadCounter.get() == 2
    }

    def "batched query with per request context batches all queries within the request"() {
        setup:
        servlet = configureServlet(ContextSetting.PER_REQUEST)
        request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')
        fetchCounter.set(0)
        loadCounter.set(0)

        when:
        servlet.doGet(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
        fetchCounter.get() == 1
        loadCounter.get() == 2
    }

    List<Map<String, Object>> getBatchedResponseContent() {
        mapper.readValue(response.getContentAsByteArray(), List)
    }
}
