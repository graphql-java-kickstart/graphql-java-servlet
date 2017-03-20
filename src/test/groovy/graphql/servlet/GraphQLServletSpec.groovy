package graphql.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.Scalars
import graphql.execution.SimpleExecutionStrategy
import graphql.schema.DataFetcher
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Andrew Potter
 */
class GraphQLServletSpec extends Specification {

    public static final int STATUS_OK = 200
    public static final int STATUS_BAD_REQUEST = 400
    public static final int STATUS_ERROR = 500
    public static final String CONTENT_TYPE_JSON_UTF8 = 'application/json;charset=UTF-8'

    @Shared
    ObjectMapper mapper = new ObjectMapper()

    GraphQLServlet servlet
    MockHttpServletRequest request
    MockHttpServletResponse response

    def setup() {
        servlet = createServlet()
        request = new MockHttpServletRequest()
        response = new MockHttpServletResponse()
    }

    def createServlet(DataFetcher queryDataFetcher = { env -> env.arguments.arg }, DataFetcher mutationDataFetcher = { env -> env.arguments.arg }) {
        GraphQLObjectType query = GraphQLObjectType.newObject()
            .name("Query")
            .field { field ->
            field.name("echo")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("arg")
                argument.type(Scalars.GraphQLString)
            }
            field.dataFetcher(queryDataFetcher)
        }
        .build()

        GraphQLObjectType mutation = GraphQLObjectType.newObject()
            .name("Mutation")
            .field { field ->
            field.name("echo")
            field.type(Scalars.GraphQLString)
            field.argument { argument ->
                argument.name("arg")
                argument.type(Scalars.GraphQLString)
            }
            field.dataFetcher(mutationDataFetcher)
        }
        .build()

        return new SimpleGraphQLServlet(new GraphQLSchema(query, mutation, [query, mutation].toSet()), new SimpleExecutionStrategy())
    }

    Map<String, Object> getResponseContent() {
        mapper.readValue(response.getContentAsByteArray(), Map)
    }

    def "HTTP GET without info returns bad request"() {
        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_BAD_REQUEST
    }

    def "HTTP GET to /schema.json returns introspection query"() {
        setup:
            request.setPathInfo('/schema.json')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.__schema != null
    }

    def "query over HTTP GET returns data"() {
        setup:
            request.addParameter('query', 'query { echo(arg:"test") }')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP GET with variables returns data"() {
        setup:
            request.addParameter('query', 'query Echo($arg: String) { echo(arg:$arg) }')
            request.addParameter('variables', '{"arg": "test"}')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP GET with operationName returns data"() {
        when:
            response = new MockHttpServletResponse()
            request.addParameter('query', 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }')
            request.addParameter('operationName', 'two')
            servlet.doGet(request, response)
        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echoOne == null
            getResponseContent().data.echoTwo == "test-two"

    }

    def "mutation over HTTP GET returns errors"() {
        setup:
            request.addParameter('query', 'mutation { echo(arg:"test") }')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().errors.size() == 1
    }

    def "query over HTTP POST without part or body returns bad request"() {
        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_BAD_REQUEST
    }

    def "query over HTTP POST body returns data"() {
        setup:
            request.setContent(mapper.writeValueAsBytes([
                query: 'query { echo(arg:"test") }'
            ]))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST body with variables returns data"() {
        setup:
            request.setContent(mapper.writeValueAsBytes([
                query: 'query Echo($arg: String) { echo(arg:$arg) }',
                variables: '{"arg": "test"}'
            ]))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST body with operationName returns data"() {
        setup:
            request.setContent(mapper.writeValueAsBytes([
                query: 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }',
                operationName: 'two'
            ]))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echoOne == null
            getResponseContent().data.echoTwo == "test-two"
    }

    def "query over HTTP POST multipart returns data"() {
        setup:
            request.setContentType("multipart/graphql, boundary=Test")
            request.setMethod("POST")
            request.addPart(new TestMultipartPart(name: 'graphql', content: mapper.writeValueAsString([
                query: 'query { echo(arg:"test") }'
            ])))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart with variables returns data"() {
        setup:
            request.setContentType("multipart/graphql")
            request.setMethod("POST")
            request.addPart(new TestMultipartPart(name: 'graphql', content: mapper.writeValueAsString([
                query: 'query Echo($arg: String) { echo(arg:$arg) }',
                variables: '{"arg": "test"}'
            ])))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart with operationName returns data"() {
        setup:
            request.setContentType("multipart/graphql")
            request.setMethod("POST")
            request.addPart(new TestMultipartPart(name: 'graphql', content: mapper.writeValueAsString([
                query: 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }',
                operationName: 'two'
            ])))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echoOne == null
            getResponseContent().data.echoTwo == "test-two"
    }

    def "mutation over HTTP POST body returns data"() {
        setup:
            request.setContent(mapper.writeValueAsBytes([
                query: 'mutation { echo(arg:"test") }'
            ]))

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "errors before graphql schema execution return internal server error"() {
        setup:
            servlet = new SimpleGraphQLServlet(servlet.schema, servlet.executionStrategy) {
                @Override
                GraphQLSchema getSchema() {
                    throw new TestException()
                }
            }

            request.setPathInfo('/schema.json')

        when:
            servlet.doGet(request, response)

        then:
            noExceptionThrown()
            response.getStatus() == STATUS_ERROR
    }

    def "errors while data fetching are masked in the response"() {
        setup:
            servlet = createServlet({ throw new TestException() })
            request.addParameter('query', 'query { echo(arg:"test") }')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            def errors = getResponseContent().errors
            errors.size() == 1
            errors.first().message.startsWith("Internal Server Error(s)")
    }

    def "data field is present and null if no data can be returned"() {
        setup:
            request.addParameter('query', 'query { not-a-field(arg:"test") }')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            def resp = getResponseContent()
            resp.containsKey("data")
            resp.data == null
            resp.errors != null
    }
}
