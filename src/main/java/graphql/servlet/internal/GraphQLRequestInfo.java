package graphql.servlet.internal;

import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLContext;

/**
 * @author Andrew Potter
 */
public class GraphQLRequestInfo {
    private final GraphQLSchema schema;
    private final GraphQLContext context;
    private final Object root;

    public GraphQLRequestInfo(GraphQLSchema schema, GraphQLContext context, Object root) {
        this.schema = schema;
        this.context = context;
        this.root = root;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public GraphQLContext getContext() {
        return context;
    }

    public Object getRoot() {
        return root;
    }
}
