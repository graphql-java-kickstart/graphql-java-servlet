package graphql.servlet.input;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.GraphQLSchema;
import graphql.servlet.context.GraphQLContext;
import graphql.servlet.core.internal.GraphQLRequest;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

/**
 * @author Andrew Potter
 */
public class GraphQLSingleInvocationInput {

    private final GraphQLSchema schema;

    private final ExecutionInput executionInput;

    private final Optional<Subject> subject;

    public GraphQLSingleInvocationInput(GraphQLRequest request, GraphQLSchema schema, GraphQLContext context, Object root) {
        this.schema = schema;
        this.executionInput = createExecutionInput(request, context, root);
        subject = context.getSubject();
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public Optional<Subject> getSubject() {
        return subject;
    }

    protected ExecutionInput createExecutionInput(GraphQLRequest graphQLRequest, GraphQLContext context, Object root) {
        return ExecutionInput.newExecutionInput()
            .query(graphQLRequest.getQuery())
            .operationName(graphQLRequest.getOperationName())
            .context(context)
            .root(root)
            .variables(graphQLRequest.getVariables())
            .dataLoaderRegistry(context.getDataLoaderRegistry().orElse(new DataLoaderRegistry()))
            .executionId(ExecutionId.generate())
            .build();
    }

    public ExecutionInput getExecutionInput() {
        return executionInput;
    }
}
