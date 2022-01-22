package graphql.kickstart.execution.context;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.PerQueryBatchedInvocationInput;
import graphql.kickstart.execution.input.PerRequestBatchedInvocationInput;
import graphql.kickstart.execution.instrumentation.ConfigurableDispatchInstrumentation;
import graphql.kickstart.execution.instrumentation.FieldLevelTrackingApproach;
import graphql.kickstart.execution.instrumentation.RequestLevelTrackingApproach;
import graphql.schema.GraphQLSchema;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.dataloader.DataLoaderRegistry;

/**
 * An enum representing possible context settings. These are modeled after Apollo's link settings.
 */
public enum ContextSetting {

  /**
   * A context object, and therefor dataloader registry and subject, should be shared between all
   * GraphQL executions in a http request.
   */
  PER_REQUEST_WITH_INSTRUMENTATION,
  PER_REQUEST_WITHOUT_INSTRUMENTATION,
  /** Each GraphQL execution should always have its own context. */
  PER_QUERY_WITH_INSTRUMENTATION,
  PER_QUERY_WITHOUT_INSTRUMENTATION;

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
      case PER_QUERY_WITH_INSTRUMENTATION:
        // Intentional fallthrough
      case PER_QUERY_WITHOUT_INSTRUMENTATION:
        return new PerQueryBatchedInvocationInput(requests, schema, contextSupplier, root, this);
      case PER_REQUEST_WITHOUT_INSTRUMENTATION:
        // Intentional fallthrough
      case PER_REQUEST_WITH_INSTRUMENTATION:
        return new PerRequestBatchedInvocationInput(requests, schema, contextSupplier, root, this);
      default:
        throw new ContextSettingNotConfiguredException();
    }
  }

  /**
   * Augments the provided instrumentation supplier to also supply the correct dispatching
   * instrumentation.
   *
   * @param instrumentation the instrumentation supplier to augment
   * @param executionInputs the inputs that will be dispatched by the instrumentation
   * @param options the DataLoader dispatching instrumentation options that will be used.
   * @return augmented instrumentation supplier.
   */
  public Supplier<Instrumentation> configureInstrumentationForContext(
      Supplier<Instrumentation> instrumentation,
      List<ExecutionInput> executionInputs,
      DataLoaderDispatcherInstrumentationOptions options) {
    ConfigurableDispatchInstrumentation dispatchInstrumentation;
    switch (this) {
      case PER_REQUEST_WITH_INSTRUMENTATION:
        DataLoaderRegistry registry =
            executionInputs.stream()
                .findFirst()
                .map(ExecutionInput::getDataLoaderRegistry)
                .orElseThrow(IllegalArgumentException::new);
        List<ExecutionId> executionIds =
            executionInputs.stream()
                .map(ExecutionInput::getExecutionId)
                .collect(Collectors.toList());
        RequestLevelTrackingApproach requestTrackingApproach =
            new RequestLevelTrackingApproach(executionIds, registry);
        dispatchInstrumentation =
            new ConfigurableDispatchInstrumentation(
                options, (dataLoaderRegistry -> requestTrackingApproach));
        break;
      case PER_QUERY_WITH_INSTRUMENTATION:
        dispatchInstrumentation =
            new ConfigurableDispatchInstrumentation(options, FieldLevelTrackingApproach::new);
        break;
      case PER_REQUEST_WITHOUT_INSTRUMENTATION:
        // Intentional fallthrough
      case PER_QUERY_WITHOUT_INSTRUMENTATION:
        return instrumentation;
      default:
        throw new ContextSettingNotConfiguredException();
    }
    return () ->
        new ChainedInstrumentation(Arrays.asList(dispatchInstrumentation, instrumentation.get()));
  }
}
