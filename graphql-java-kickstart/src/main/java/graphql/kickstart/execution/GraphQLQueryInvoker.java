package graphql.kickstart.execution;

import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.execution.config.DefaultExecutionStrategyProvider;
import graphql.kickstart.execution.config.ExecutionStrategyProvider;
import graphql.kickstart.execution.config.GraphQLBuilder;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Andrew Potter
 */
public class GraphQLQueryInvoker {

  private final Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider;
  private final Supplier<Instrumentation> getInstrumentation;
  private final Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider;
  private final Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier;

  protected GraphQLQueryInvoker(
      Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider,
      Supplier<Instrumentation> getInstrumentation,
      Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider,
      Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier) {
    this.getExecutionStrategyProvider = getExecutionStrategyProvider;
    this.getInstrumentation = getInstrumentation;
    this.getPreparsedDocumentProvider = getPreparsedDocumentProvider;
    this.optionsSupplier = optionsSupplier;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public GraphQLInvoker toGraphQLInvoker() {
    GraphQLBuilder graphQLBuilder =
        new GraphQLBuilder()
            .executionStrategyProvider(getExecutionStrategyProvider)
            .instrumentation(getInstrumentation)
            .preparsedDocumentProvider(getPreparsedDocumentProvider);
    return new GraphQLInvoker(graphQLBuilder, new BatchedDataLoaderGraphQLBuilder(optionsSupplier));
  }

  public static class Builder {

    private Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider =
        DefaultExecutionStrategyProvider::new;
    private Supplier<Instrumentation> getInstrumentation =
        () -> SimplePerformantInstrumentation.INSTANCE;
    private Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider =
        () -> NoOpPreparsedDocumentProvider.INSTANCE;
    private Supplier<DataLoaderDispatcherInstrumentationOptions>
        dataLoaderDispatcherInstrumentationOptionsSupplier =
            DataLoaderDispatcherInstrumentationOptions::newOptions;

    public Builder withExecutionStrategyProvider(ExecutionStrategyProvider provider) {
      return withExecutionStrategyProvider(() -> provider);
    }

    public Builder withExecutionStrategyProvider(Supplier<ExecutionStrategyProvider> supplier) {
      this.getExecutionStrategyProvider = supplier;
      return this;
    }

    public Builder withInstrumentation(Instrumentation instrumentation) {
      return withInstrumentation(() -> instrumentation);
    }

    public Builder withInstrumentation(Supplier<Instrumentation> supplier) {
      this.getInstrumentation = supplier;
      return this;
    }

    public Builder with(List<Instrumentation> instrumentations) {
      if (instrumentations.isEmpty()) {
        return this;
      }
      if (instrumentations.size() == 1) {
        withInstrumentation(instrumentations.get(0));
      } else {
        withInstrumentation(new ChainedInstrumentation(instrumentations));
      }
      return this;
    }

    public Builder withPreparsedDocumentProvider(PreparsedDocumentProvider provider) {
      return withPreparsedDocumentProvider(() -> provider);
    }

    public Builder withPreparsedDocumentProvider(Supplier<PreparsedDocumentProvider> supplier) {
      this.getPreparsedDocumentProvider = supplier;
      return this;
    }

    public Builder withDataLoaderDispatcherInstrumentationOptions(
        DataLoaderDispatcherInstrumentationOptions options) {
      return withDataLoaderDispatcherInstrumentationOptions(() -> options);
    }

    public Builder withDataLoaderDispatcherInstrumentationOptions(
        Supplier<DataLoaderDispatcherInstrumentationOptions> supplier) {
      this.dataLoaderDispatcherInstrumentationOptionsSupplier = supplier;
      return this;
    }

    public GraphQLQueryInvoker build() {
      return new GraphQLQueryInvoker(
          getExecutionStrategyProvider,
          getInstrumentation,
          getPreparsedDocumentProvider,
          dataLoaderDispatcherInstrumentationOptionsSupplier);
    }
  }
}
