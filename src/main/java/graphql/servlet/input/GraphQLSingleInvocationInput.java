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
 *  Represents a single GraphQL execution.
 */
public class GraphQLSingleInvocationInput implements GraphQLInvocationInput {

    private final GraphQLSchema schema;

    private final ExecutionInput executionInput;

    private final Optional<Subject> subject;

    public GraphQLSingleInvocationInput(GraphQLRequest request, GraphQLSchema schema, GraphQLContext context, Object root) {
        this.schema = schema;
        this.executionInput = createExecutionInput(request, context, root);
        subject = context.getSubject();
    }

    /**
     * @return the schema to use to execute this query.
     */
    public GraphQLSchema getSchema() {
        return schema;
    }

    /**
     * @return a subject to execute the query as.
     */
    public Optional<Subject> getSubject() {
        return subject;
    }

    private ExecutionInput createExecutionInput(GraphQLRequest graphQLRequest, GraphQLContext context, Object root) {
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
