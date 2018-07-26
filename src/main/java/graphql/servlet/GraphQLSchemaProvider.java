package graphql.servlet;

import graphql.schema.GraphQLSchema;

import javax.servlet.http.HttpServletRequest;

public interface GraphQLSchemaProvider {

    static GraphQLSchema copyReadOnly(GraphQLSchema schema) {
        return GraphQLSchema.newSchema().query(schema.getQueryType()).subscription(schema.getSubscriptionType()).build(schema.getAdditionalTypes());
    }

    /**
     * @param request the http request
     * @return a schema based on the request (auth, etc).  Optional is empty when called from an mbean.
     */
    GraphQLSchema getSchema(HttpServletRequest request);


    /**
     * @return a schema for handling mbean calls.
     */
    GraphQLSchema getSchema();

    /**
     * @param request the http request
     * @return a read-only schema based on the request (auth, etc).  Should return the same schema as {@link #getSchema(HttpServletRequest)} for a given request.
     */
    GraphQLSchema getReadOnlySchema(HttpServletRequest request);
}
