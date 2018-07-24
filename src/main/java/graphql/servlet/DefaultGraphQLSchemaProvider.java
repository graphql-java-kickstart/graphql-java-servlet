package graphql.servlet;

import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

/**
 * @author Andrew Potter
 */
public class DefaultGraphQLSchemaProvider implements GraphQLSchemaProvider {

    private final GraphQLSchema schema;
    private final GraphQLSchema readOnlySchema;

    public DefaultGraphQLSchemaProvider(GraphQLSchema schema) {
        this(schema, GraphQLSchemaProvider.copyReadOnly(schema));
    }

    public DefaultGraphQLSchemaProvider(GraphQLSchema schema, GraphQLSchema readOnlySchema) {
        this.schema = schema;
        this.readOnlySchema = readOnlySchema;
    }


    @Override
    public GraphQLSchema getSchema(HttpServletRequest request) {
        return getSchema();
    }

    @Override
    public GraphQLSchema getSchema(HandshakeRequest request) {
        return getSchema();
    }

    @Override
    public GraphQLSchema getSchema() {
        return schema;
    }

    @Override
    public GraphQLSchema getReadOnlySchema(HttpServletRequest request) {
        return readOnlySchema;
    }
}
