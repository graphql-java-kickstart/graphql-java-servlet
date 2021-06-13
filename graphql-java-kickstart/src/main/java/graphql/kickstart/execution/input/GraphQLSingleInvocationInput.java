package graphql.kickstart.execution.input;

import static java.util.Collections.singletonList;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Optional;
import javax.security.auth.Subject;

/** Represents a single GraphQL execution. */
public class GraphQLSingleInvocationInput implements GraphQLInvocationInput {

  private final GraphQLSchema schema;

  private final ExecutionInput executionInput;

  private final Subject subject;

  public GraphQLSingleInvocationInput(
      GraphQLRequest request, GraphQLSchema schema, GraphQLContext context, Object root) {
    this.schema = schema;
    this.executionInput = createExecutionInput(request, context, root);
    subject = context.getSubject().orElse(null);
  }

  /** @return the schema to use to execute this query. */
  public GraphQLSchema getSchema() {
    return schema;
  }

  /** @return a subject to execute the query as. */
  public Optional<Subject> getSubject() {
    return Optional.ofNullable(subject);
  }

  private ExecutionInput createExecutionInput(
      GraphQLRequest graphQLRequest, GraphQLContext context, Object root) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLRequest.getQuery())
        .operationName(graphQLRequest.getOperationName())
        .context(context)
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
