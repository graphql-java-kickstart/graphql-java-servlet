package graphql.kickstart.execution;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.execution.preparsed.NoOpPreparsedDocumentProvider;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;
import graphql.kickstart.execution.config.DefaultExecutionStrategyProvider;
import graphql.kickstart.execution.config.ExecutionStrategyProvider;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.security.auth.Subject;

/**
 * @author Andrew Potter
 */
public class GraphQLQueryInvoker {

  private final Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider;
  private final Supplier<Instrumentation> getInstrumentation;
  private final Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider;
  private final Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier;

  protected GraphQLQueryInvoker(Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider,
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

  public GraphQLQueryResult query(GraphQLInvocationInput invocationInput) {
    if (invocationInput instanceof GraphQLSingleInvocationInput) {
      return GraphQLQueryResult.create(query((GraphQLSingleInvocationInput) invocationInput));
    }
    GraphQLBatchedInvocationInput batchedInvocationInput = (GraphQLBatchedInvocationInput) invocationInput;
    return GraphQLQueryResult.create(query(batchedInvocationInput.getInvocationInputs(), batchedInvocationInput.getContextSetting()));
  }

  public ExecutionResult query(GraphQLSingleInvocationInput singleInvocationInput) {
    return queryAsync(singleInvocationInput, getInstrumentation).join();
  }

  private CompletableFuture<ExecutionResult> queryAsync(GraphQLSingleInvocationInput singleInvocationInput,
      Supplier<Instrumentation> configuredInstrumentation) {
    return query(singleInvocationInput, configuredInstrumentation, singleInvocationInput.getExecutionInput());
  }

  public List<ExecutionResult> query(List<GraphQLSingleInvocationInput> batchedInvocationInput,
      ContextSetting contextSetting) {
    List<ExecutionInput> executionIds = batchedInvocationInput.stream()
        .map(GraphQLSingleInvocationInput::getExecutionInput)
        .collect(Collectors.toList());
    Supplier<Instrumentation> configuredInstrumentation = contextSetting
        .configureInstrumentationForContext(getInstrumentation, executionIds, optionsSupplier.get());
    return batchedInvocationInput.stream()
        .map(input -> this.queryAsync(input, configuredInstrumentation))
        //We want eager eval
        .collect(Collectors.toList())
        .stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
  }

  private GraphQL newGraphQL(GraphQLSchema schema, Supplier<Instrumentation> configuredInstrumentation) {
    ExecutionStrategyProvider executionStrategyProvider = getExecutionStrategyProvider.get();
    GraphQL.Builder builder = GraphQL.newGraphQL(schema)
        .queryExecutionStrategy(executionStrategyProvider.getQueryExecutionStrategy())
        .mutationExecutionStrategy(executionStrategyProvider.getMutationExecutionStrategy())
        .subscriptionExecutionStrategy(executionStrategyProvider.getSubscriptionExecutionStrategy())
        .preparsedDocumentProvider(getPreparsedDocumentProvider.get());
    Instrumentation instrumentation = configuredInstrumentation.get();
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

  private CompletableFuture<ExecutionResult> query(GraphQLSingleInvocationInput invocationInput,
      Supplier<Instrumentation> configuredInstrumentation, ExecutionInput executionInput) {
    if (Subject.getSubject(AccessController.getContext()) == null && invocationInput.getSubject().isPresent()) {
      return Subject
          .doAs(invocationInput.getSubject().get(), (PrivilegedAction<CompletableFuture<ExecutionResult>>) () -> {
            try {
              return query(invocationInput.getSchema(), executionInput, configuredInstrumentation);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }

    return query(invocationInput.getSchema(), executionInput, configuredInstrumentation);
  }

  private CompletableFuture<ExecutionResult> query(GraphQLSchema schema, ExecutionInput executionInput,
      Supplier<Instrumentation> configuredInstrumentation) {
    return newGraphQL(schema, configuredInstrumentation).executeAsync(executionInput);
  }

  public static class Builder {

    private Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider = DefaultExecutionStrategyProvider::new;
    private Supplier<Instrumentation> getInstrumentation = () -> SimpleInstrumentation.INSTANCE;
    private Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider = () -> NoOpPreparsedDocumentProvider.INSTANCE;
    private Supplier<DataLoaderDispatcherInstrumentationOptions> dataLoaderDispatcherInstrumentationOptionsSupplier = DataLoaderDispatcherInstrumentationOptions::newOptions;


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

    public Builder withDataLoaderDispatcherInstrumentationOptions(DataLoaderDispatcherInstrumentationOptions options) {
      return withDataLoaderDispatcherInstrumentationOptions(() -> options);
    }

    public Builder withDataLoaderDispatcherInstrumentationOptions(
        Supplier<DataLoaderDispatcherInstrumentationOptions> supplier) {
      this.dataLoaderDispatcherInstrumentationOptionsSupplier = supplier;
      return this;
    }

    public GraphQLQueryInvoker build() {
      return new GraphQLQueryInvoker(getExecutionStrategyProvider, getInstrumentation, getPreparsedDocumentProvider,
          dataLoaderDispatcherInstrumentationOptionsSupplier);
    }
  }
}
