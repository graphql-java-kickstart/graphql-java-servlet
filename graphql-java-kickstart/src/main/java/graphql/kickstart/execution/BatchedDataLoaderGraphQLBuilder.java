package graphql.kickstart.execution;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.kickstart.execution.config.GraphQLBuilder;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.util.List;
import java.util.function.Supplier;

public class BatchedDataLoaderGraphQLBuilder {

  private final Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier;

  public BatchedDataLoaderGraphQLBuilder(
      Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier) {
    if (optionsSupplier != null) {
      this.optionsSupplier = optionsSupplier;
    } else {
      this.optionsSupplier = DataLoaderDispatcherInstrumentationOptions::newOptions;
    }
  }

  GraphQL newGraphQL(GraphQLBatchedInvocationInput invocationInput, GraphQLBuilder graphQLBuilder) {
    Supplier<Instrumentation> supplier =
        augment(invocationInput, graphQLBuilder.getInstrumentationSupplier());
    return invocationInput.getInvocationInputs().stream()
        .findFirst()
        .map(GraphQLSingleInvocationInput::getSchema)
        .map(schema -> graphQLBuilder.build(schema, supplier))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Batched invocation input must contain at least one query"));
  }

  private Supplier<Instrumentation> augment(
      GraphQLBatchedInvocationInput batchedInvocationInput,
      Supplier<Instrumentation> instrumentationSupplier) {
    List<ExecutionInput> executionInputs = batchedInvocationInput.getExecutionInputs();
    return batchedInvocationInput
        .getContextSetting()
        .configureInstrumentationForContext(
            instrumentationSupplier, executionInputs, optionsSupplier.get());
  }
}
