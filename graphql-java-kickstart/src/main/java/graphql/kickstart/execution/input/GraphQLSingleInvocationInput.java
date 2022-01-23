package graphql.kickstart.execution.input;

import static java.util.Collections.singletonList;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.schema.GraphQLSchema;
import java.util.List;

/** Represents a single GraphQL execution. */
public class GraphQLSingleInvocationInput implements GraphQLInvocationInput {

  private final GraphQLSchema schema;

  private final ExecutionInput executionInput;

  public GraphQLSingleInvocationInput(
      GraphQLRequest request, GraphQLSchema schema, GraphQLKickstartContext context, Object root) {
    this.schema = schema;
    this.executionInput = createExecutionInput(request, context, root);
  }

  /** @return the schema to use to execute this query. */
  public GraphQLSchema getSchema() {
    return schema;
  }

  private ExecutionInput createExecutionInput(
      GraphQLRequest graphQLRequest, GraphQLKickstartContext context, Object root) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLRequest.getQuery())
        .operationName(graphQLRequest.getOperationName())
        .context(context)
        .graphQLContext(context.getMapOfContext())
        .root(root)
        .variables(graphQLRequest.getVariables())
        .extensions(graphQLRequest.getExtensions())
        .dataLoaderRegistry(context.getDataLoaderRegistry())
        .executionId(ExecutionId.generate())
        .build();
  }

  public ExecutionInput getExecutionInput() {
    return executionInput;
  }

  @Override
  public List<String> getQueries() {
    return singletonList(executionInput.getQuery());
  }
}
