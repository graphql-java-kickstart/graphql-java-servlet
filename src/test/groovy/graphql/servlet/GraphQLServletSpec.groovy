/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
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
            field.dataFetcher({ env -> null })
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
                query    : 'query Echo($arg: String) { echo(arg:$arg) }',
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
                query        : 'query one{ echoOne: echo(arg:"test-one") } query two{ echoTwo: echo(arg:"test-two") }',
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
                query        : 'query echo{ echo: echo(arg:"test") }',
                operationName: ''
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

    def "NonNullableFieldWasNullException is masked by default"() {
        setup:
        request.addParameter('query', 'query { returnsNullIncorrectly }')

        when:
        servlet.doGet(request, response)

        then:
        response.getStatus() == STATUS_OK
        response.getContentType() == CONTENT_TYPE_JSON_UTF8
        def resp = getResponseContent()
        resp.containsKey("data")
        resp.data == null
        resp.errors != null
        resp.errors.first().message.contains('Internal Server Error')
    }

    def "typeInfo is serialized correctly"() {
        expect:
        GraphQLServlet.mapper.writeValueAsString(ExecutionTypeInfo.newTypeInfo().type(new GraphQLNonNull(Scalars.GraphQLString)).build()) != "{}"
    }
}
