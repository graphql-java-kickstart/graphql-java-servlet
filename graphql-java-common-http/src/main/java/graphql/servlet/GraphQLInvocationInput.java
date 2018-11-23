package graphql.servlet;

import graphql.ExecutionInput;
import graphql.schema.GraphQLSchema;
import graphql.servlet.internal.GraphQLRequest;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

/**
 * @author Andrew Potter
 */
public abstract class GraphQLInvocationInput {

    private final GraphQLSchema schema;
    private final GraphQLContext context;
    private final Object root;

    public GraphQLInvocationInput(GraphQLSchema schema, GraphQLContext context, Object root) {
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

    public Optional<Subject> getSubject() {
        return context.getSubject();
    }

    protected ExecutionInput createExecutionInput(GraphQLRequest graphQLRequest) {
        return ExecutionInput.newExecutionInput()
                .query(graphQLRequest.getQuery())
                .operationName(graphQLRequest.getOperationName())
                .context(context)
                .root(root)
                .variables(graphQLRequest.getVariables())
                .dataLoaderRegistry(context.getDataLoaderRegistry().orElse(new DataLoaderRegistry()))
                .build();
    }

}
