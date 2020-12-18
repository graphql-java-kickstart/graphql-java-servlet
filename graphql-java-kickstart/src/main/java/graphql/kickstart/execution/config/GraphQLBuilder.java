package graphql.kickstart.execution.config;

import graphql.GraphQL;
import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;
import java.util.function.Supplier;
import lombok.Getter;

public class GraphQLBuilder {

  private Supplier<ExecutionStrategyProvider> executionStrategyProviderSupplier = DefaultExecutionStrategyProvider::new;
  private Supplier<PreparsedDocumentProvider> preparsedDocumentProviderSupplier = () -> NoOpPreparsedDocumentProvider.INSTANCE;
  @Getter
  private Supplier<Instrumentation> instrumentationSupplier = () -> SimpleInstrumentation.INSTANCE;

  public GraphQLBuilder executionStrategyProvider(Supplier<ExecutionStrategyProvider> supplier) {
    if (supplier != null) {
      executionStrategyProviderSupplier = supplier;
    }
    return this;
  }

  public GraphQLBuilder preparsedDocumentProvider(Supplier<PreparsedDocumentProvider> supplier) {
    if (supplier != null) {
      preparsedDocumentProviderSupplier = supplier;
    }
    return this;
  }

  public GraphQLBuilder instrumentation(Supplier<Instrumentation> supplier) {
    if (supplier != null) {
      instrumentationSupplier = supplier;
    }
    return this;
  }

  public GraphQL build(GraphQLSchemaProvider schemaProvider) {
    return build(schemaProvider.getSchema());
  }

  public GraphQL build(GraphQLSchema schema) {
    return build(schema, instrumentationSupplier);
  }

  public GraphQL build(GraphQLSchema schema,
      Supplier<Instrumentation> configuredInstrumentationSupplier) {
    ExecutionStrategyProvider executionStrategyProvider = executionStrategyProviderSupplier.get();
    ExecutionStrategy queryExecutionStrategy = executionStrategyProvider
        .getQueryExecutionStrategy();
    ExecutionStrategy mutationExecutionStrategy = executionStrategyProvider
        .getMutationExecutionStrategy();
    ExecutionStrategy subscriptionExecutionStrategy = executionStrategyProvider
        .getSubscriptionExecutionStrategy();

    GraphQL.Builder builder = GraphQL.newGraphQL(schema)
        .preparsedDocumentProvider(preparsedDocumentProviderSupplier.get());

    if (queryExecutionStrategy != null) {
      builder.queryExecutionStrategy(queryExecutionStrategy);
    }

    if (mutationExecutionStrategy != null) {
      builder.mutationExecutionStrategy(mutationExecutionStrategy);
    }

    if (subscriptionExecutionStrategy != null) {
      builder.subscriptionExecutionStrategy(subscriptionExecutionStrategy);
    }

    Instrumentation instrumentation = configuredInstrumentationSupplier.get();
    builder.instrumentation(instrumentation);
    if (containsDispatchInstrumentation(instrumentation)) {
      builder.doNotAddDefaultInstrumentations();
    }
    return builder.build();
  }

  private boolean containsDispatchInstrumentation(Instrumentation instrumentation) {
    if (instrumentation instanceof ChainedInstrumentation) {
      return ((ChainedInstrumentation) instrumentation).getInstrumentations().stream()
          .anyMatch(this::containsDispatchInstrumentation);
    }
    return instrumentation instanceof DataLoaderDispatcherInstrumentation;
  }

}
