package graphql.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.Scalars
import graphql.execution.ExecutionTypeInfo
import graphql.schema.DataFetcher
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
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
            .field { GraphQLFieldDefinition.Builder field ->
                field.name("echo")
                field.type(Scalars.GraphQLString)
                field.argument { argument ->
                    argument.name("arg")
                    argument.type(Scalars.GraphQLString)
                }
                field.dataFetcher(queryDataFetcher)
            }
            .field { GraphQLFieldDefinition.Builder field ->
                field.name("returnsNullIncorrectly")
                field.type(new GraphQLNonNull(Scalars.GraphQLString))
                field.dataFetcher({env -> null})
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

        return new SimpleGraphQLServlet(new GraphQLSchema(query, mutation, [query, mutation].toSet()))
    }

    Map<String, Object> getResponseContent() {
        mapper.readValue(response.getContentAsByteArray(), Map)
    }

    List<Map<String, Object>> getBatchedResponseContent() {
        mapper.readValue(response.getContentAsByteArray(), List)
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

    def "query over HTTP GET with variables as string returns data"() {
        setup:
            request.addParameter('query', 'query Echo($arg: String) { echo(arg:$arg) }')
            request.addParameter('variables', '"{\\"arg\\": \\"test\\"}"')

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

    def "query over HTTP GET with empty non-null operationName returns data"() {
        when:
        response = new MockHttpServletResponse()
        request.addParameter('query', 'query echo{ echo: echo(arg:"test") }')
        request.addParameter('operationName', '')
        servlet.doGet(request, response)
        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getResponseContent().data.echo == "test"
    }

    def "query over HTTP GET with unknown property 'test' returns data"() {
        setup:
        request.addParameter('query', 'query { echo(arg:"test") }')
        request.addParameter('test', 'test')

        when:
        servlet.doGet(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getResponseContent().data.echo == "test"
    }

    def "batched query over HTTP GET returns data"() {
        setup:
            request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP GET with variables returns data"() {
        setup:
            request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }, { "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }]')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP GET with variables as string returns data"() {
        setup:
            request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }", "variables": "{ \\"arg\\": \\"test\\" }" }, { "query": "query { echo(arg:\\"test\\") }", "variables": "{ \\"arg\\": \\"test\\" }" }]')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP GET with operationName returns data"() {
        when:
            response = new MockHttpServletResponse()
            request.addParameter('query', '[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]')
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echoOne == "test-one"
            getBatchedResponseContent()[0].data.echoTwo == null
            getBatchedResponseContent()[1].data.echoOne == null
            getBatchedResponseContent()[1].data.echoTwo == "test-two"
    }

    def "batched query over HTTP GET with empty non-null operationName returns data"() {
        when:
            response = new MockHttpServletResponse()
            request.addParameter('query', '[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]')
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP GET with unknown property 'test' returns data"() {
        setup:
        request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]')

        when:
        servlet.doGet(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
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

    def "batched mutation over HTTP GET returns errors"() {
        setup:
            request.addParameter('query', '[{ "query": "mutation { echo(arg:\\"test\\") }" }, { "query": "mutation {echo(arg:\\"test\\") }" }]')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].errors.size() == 1
            getBatchedResponseContent()[1].errors.size() == 1
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

    def "query over HTTP POST body with graphql contentType returns data"() {
        setup:
        request.addHeader("Content-Type", "application/graphql")
        request.setContent('query { echo(arg:"test") }'.getBytes("UTF-8"))

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

    def "query over HTTP POST body with empty non-null operationName returns data"() {
        setup:
        request.setContent(mapper.writeValueAsBytes([
                query: 'query echo{ echo: echo(arg:"test") }',
                operationName: ''
        ]))

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST body with unknown property 'test' returns data"() {
        setup:
        request.setContent(mapper.writeValueAsBytes([
                query: 'query { echo(arg:"test") }',
                test: 'test'
        ]))

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart named 'graphql' returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")

            request.setContent(new TestMultipartContentBuilder()
                .addPart('graphql', mapper.writeValueAsString([query: 'query { echo(arg:"test") }']))
                .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart named 'query' returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                .addPart('query', 'query { echo(arg:"test") }')
                .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart named 'query' with operationName returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                .addPart('query', 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }')
                .addPart('operationName', 'two')
                .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echoOne == null
            getResponseContent().data.echoTwo == "test-two"
    }

    def "query over HTTP POST multipart named 'query' with empty non-null operationName returns data"() {
        setup:
        request.setContentType("multipart/form-data, boundary=test")
        request.setMethod("POST")
        request.setContent(new TestMultipartContentBuilder()
                .addPart('query', 'query echo{ echo: echo(arg:"test") }')
                .addPart('operationName', '')
                .build())

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart named 'query' with variables returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                .addPart('query', 'query Echo($arg: String) { echo(arg:$arg) }')
                .addPart('variables', '{"arg": "test"}')
                .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getResponseContent().data.echo == "test"
    }

    def "query over HTTP POST multipart named 'query' with unknown property 'test' returns data"() {
        setup:
        request.setContentType("multipart/form-data, boundary=test")
        request.setMethod("POST")
        request.setContent(new TestMultipartContentBuilder()
                .addPart('query', 'query { echo(arg:"test") }')
                .addPart('test', 'test')
                .build())

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getResponseContent().data.echo == "test"
    }

    def "batched query over HTTP POST body returns data"() {
        setup:
            request.setContent('[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]'.bytes)

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST body with variables returns data"() {
        setup:
            request.setContent('[{ "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }, { "query": "query { echo(arg:\\"test\\") }", "variables": { "arg": "test" } }]'.bytes)

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST body with operationName returns data"() {
        setup:
            request.setContent('[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]'.bytes)

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echoOne == "test-one"
            getBatchedResponseContent()[0].data.echoTwo == null
            getBatchedResponseContent()[1].data.echoOne == null
            getBatchedResponseContent()[1].data.echoTwo == "test-two"
    }

    def "batched query over HTTP POST body with empty non-null operationName returns data"() {
        setup:
            request.setContent('[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]'.bytes)

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST body with unknown property 'test' returns data"() {
        setup:
        request.setContent('[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]'.bytes)

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST multipart named 'graphql' returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")

            request.setContent(new TestMultipartContentBuilder()
                    .addPart('graphql', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')
                    .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST multipart named 'graphql' with unknown property 'test' returns data"() {
        setup:
        request.setContentType("multipart/form-data, boundary=test")
        request.setMethod("POST")

        request.setContent(new TestMultipartContentBuilder()
                .addPart('graphql', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]')
                .build())

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST multipart named 'query' returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                    .addPart('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')
                    .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST multipart named 'query' with operationName returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                    .addPart('query', '[{ "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "one" }, { "query": "query one{ echoOne: echo(arg:\\"test-one\\") } query two{ echoTwo: echo(arg:\\"test-two\\") }", "operationName": "two" }]')
                    .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echoOne == "test-one"
            getBatchedResponseContent()[0].data.echoTwo == null
            getBatchedResponseContent()[1].data.echoOne == null
            getBatchedResponseContent()[1].data.echoTwo == "test-two"
    }

    def "batched query over HTTP POST multipart named 'query' with empty non-null operationName returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                    .addPart('query', '[{ "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }, { "query": "query echo{ echo: echo(arg:\\"test\\") }", "operationName": "" }]')
                    .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST multipart named 'query' with variables returns data"() {
        setup:
            request.setContentType("multipart/form-data, boundary=test")
            request.setMethod("POST")
            request.setContent(new TestMultipartContentBuilder()
                    .addPart('query', '[{ "query": "query echo($arg: String) { echo(arg:$arg) }", "variables": { "arg": "test" } }, { "query": "query echo($arg: String) { echo(arg:$arg) }", "variables": { "arg": "test" } }]')
                    .build())

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched query over HTTP POST multipart named 'query' with unknown property 'test' returns data"() {
        setup:
        request.setContentType("multipart/form-data, boundary=test")
        request.setMethod("POST")
        request.setContent(new TestMultipartContentBuilder()
                .addPart('query', '[{ "query": "query { echo(arg:\\"test\\") }", "test": "test" }, { "query": "query { echo(arg:\\"test\\") }", "test": "test" }]')
                .build())

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
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

    def "batched mutation over HTTP POST body returns data"() {
        setup:
            request.setContent('[{ "query": "mutation { echo(arg:\\"test\\") }" }, { "query": "mutation { echo(arg:\\"test\\") }" }]'.bytes)

        when:
            servlet.doPost(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            getBatchedResponseContent()[0].data.echo == "test"
            getBatchedResponseContent()[1].data.echo == "test"
    }

    def "batched mutation over HTTP POST body with unknown property 'test' returns data"() {
        setup:
        request.setContent('[{ "query": "mutation { echo(arg:\\"test\\") }", "test": "test" }, { "query": "mutation { echo(arg:\\"test\\") }", "test": "test" }]'.bytes)

        when:
        servlet.doPost(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        getBatchedResponseContent()[0].data.echo == "test"
        getBatchedResponseContent()[1].data.echo == "test"
    }

    def "errors before graphql schema execution return internal server error"() {
        setup:
            servlet = new SimpleGraphQLServlet(servlet.getSchemaProvider().getSchema()) {
                @Override
                GraphQLSchemaProvider getSchemaProvider() {
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

    def "errors that also implement GraphQLError thrown while data fetching are passed to caller"() {
        setup:
            servlet = createServlet({ throw new TestGraphQLErrorException("This is a test message") })
            request.addParameter('query', 'query { echo(arg:"test") }')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            def errors = getResponseContent().errors
            errors.size() == 1
            errors.first().extensions.foo == "bar"
            errors.first().message.startsWith("Exception while fetching data (/echo) : This is a test message")
    }

    def "batched errors while data fetching are masked in the response"() {
        setup:
            servlet = createServlet({ throw new TestException() })
            request.addParameter('query', '[{ "query": "query { echo(arg:\\"test\\") }" }, { "query": "query { echo(arg:\\"test\\") }" }]')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            def errors = getBatchedResponseContent().errors
            errors[0].size() == 1
            errors[0].first().message.startsWith("Internal Server Error(s)")
            errors[1].size() == 1
            errors[1].first().message.startsWith("Internal Server Error(s)")
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

    def "batched data field is present and null if no data can be returned"() {
        setup:
            request.addParameter('query', '[{ "query": "query { not-a-field(arg:\\"test\\") }" }, { "query": "query { not-a-field(arg:\\"test\\") }" }]')

        when:
            servlet.doGet(request, response)

        then:
            response.getStatus() == STATUS_OK
            response.getContentType() == CONTENT_TYPE_JSON_UTF8
            def resp = getBatchedResponseContent()
            resp[0].containsKey("data")
            resp[0].data == null
            resp[0].errors != null
            resp[1].containsKey("data")
            resp[1].data == null
            resp[1].errors != null
    }

    def "typeInfo is serialized correctly"() {
        expect:
            servlet.getMapper().writeValueAsString(ExecutionTypeInfo.newTypeInfo().type(new GraphQLNonNull(Scalars.GraphQLString)).build()) != "{}"
    }

    def "isBatchedQuery check uses buffer length as read limit"() {
        setup:
            HttpServletRequest mockRequest = Mock()
            ServletInputStream mockInputStream = Mock()

            mockInputStream.markSupported() >> true
            mockRequest.getInputStream() >> mockInputStream
            mockRequest.getMethod() >> "POST"

        when:
            servlet.doPost(mockRequest, response)

        then:
            1 * mockInputStream.mark(128)

        then:
            1 * mockInputStream.read({ it.length == 128 }) >> -1

        then:
            1 * mockInputStream.reset()
    }
}
