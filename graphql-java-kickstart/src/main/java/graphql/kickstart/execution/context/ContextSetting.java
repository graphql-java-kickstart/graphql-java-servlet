package graphql.kickstart.execution.context;

import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.PerQueryBatchedInvocationInput;
import graphql.kickstart.execution.input.PerRequestBatchedInvocationInput;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.function.Supplier;

/**
 * An enum representing possible context settings. These are modeled after Apollo's link settings.
 */
public enum ContextSetting {

  /**
   * A context object, and therefor dataloader registry and subject, should be shared between all
   * GraphQL executions in a http request.
   */
  PER_REQUEST,
  /** Each GraphQL execution should always have its own context. */
  PER_QUERY;

  /**
   * Creates a set of inputs with the correct context based on the setting.
   *
   * @param requests the GraphQL requests to execute.
   * @param schema the GraphQL schema to execute the requests against.
   * @param contextSupplier method that returns the context to use for each execution or for the
   *     request as a whole.
   * @param root the root object to use for each execution.
   * @return a configured batch input.
   */
  public GraphQLBatchedInvocationInput getBatch(
      List<GraphQLRequest> requests,
      GraphQLSchema schema,
      Supplier<GraphQLKickstartContext> contextSupplier,
      Object root) {
    switch (this) {
      case PER_QUERY:
        return new PerQueryBatchedInvocationInput(requests, schema, contextSupplier, root, this);
      case PER_REQUEST:
        return new PerRequestBatchedInvocationInput(requests, schema, contextSupplier, root, this);
      default:
        throw new ContextSettingNotConfiguredException();
    }
  }
}
