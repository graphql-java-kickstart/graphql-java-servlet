package graphql.servlet;

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
import graphql.servlet.internal.ExecutionResultHandler;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Andrew Potter
 */
public class GraphQLQueryInvoker {

    private final Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider;
    private final Supplier<Instrumentation> getInstrumentation;
    private final Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider;
    private final Supplier<DataLoaderDispatcherInstrumentationOptions> dataLoaderDispatcherInstrumentationOptionsSupplier;

    protected GraphQLQueryInvoker(Supplier<ExecutionStrategyProvider> getExecutionStrategyProvider, Supplier<Instrumentation> getInstrumentation, Supplier<PreparsedDocumentProvider> getPreparsedDocumentProvider, Supplier<DataLoaderDispatcherInstrumentationOptions> optionsSupplier) {
        this.getExecutionStrategyProvider = getExecutionStrategyProvider;
        this.getInstrumentation = getInstrumentation;
        this.getPreparsedDocumentProvider = getPreparsedDocumentProvider;
        this.dataLoaderDispatcherInstrumentationOptionsSupplier = optionsSupplier;
    }

    public ExecutionResult query(GraphQLSingleInvocationInput singleInvocationInput) {
        return query(singleInvocationInput, singleInvocationInput.getExecutionInput());
    }

    public void query(GraphQLBatchedInvocationInput batchedInvocationInput, ExecutionResultHandler executionResultHandler) {
        Iterator<ExecutionInput> executionInputIterator = batchedInvocationInput.getExecutionInputs().iterator();

        while (executionInputIterator.hasNext()) {
            ExecutionResult result = query(batchedInvocationInput, executionInputIterator.next());
            executionResultHandler.accept(result, executionInputIterator.hasNext());
        }
    }

    private GraphQL newGraphQL(GraphQLSchema schema, Object context) {
        ExecutionStrategyProvider executionStrategyProvider = getExecutionStrategyProvider.get();
        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(executionStrategyProvider.getQueryExecutionStrategy())
            .mutationExecutionStrategy(executionStrategyProvider.getMutationExecutionStrategy())
            .subscriptionExecutionStrategy(executionStrategyProvider.getSubscriptionExecutionStrategy())
            .instrumentation(getInstrumentation(context))
            .preparsedDocumentProvider(getPreparsedDocumentProvider.get())
            .build();
    }

    protected Instrumentation getInstrumentation(Object context) {
        if (context instanceof GraphQLContext) {
            return ((GraphQLContext) context).getDataLoaderRegistry()
                    .map(registry -> {
                        List<Instrumentation> instrumentations = new ArrayList<>();
                        instrumentations.add(getInstrumentation.get());
                        instrumentations.add(new DataLoaderDispatcherInstrumentation(registry, dataLoaderDispatcherInstrumentationOptionsSupplier.get()));
                        return new ChainedInstrumentation(instrumentations);
                    })
                    .map(Instrumentation.class::cast)
                    .orElse(getInstrumentation.get());
        }
        return getInstrumentation.get();
    }

    private ExecutionResult query(GraphQLInvocationInput invocationInput, ExecutionInput executionInput) {
        if (Subject.getSubject(AccessController.getContext()) == null && invocationInput.getSubject().isPresent()) {
            return Subject.doAs(invocationInput.getSubject().get(), (PrivilegedAction<ExecutionResult>) () -> {
                try {
                    return query(invocationInput.getSchema(), executionInput);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return query(invocationInput.getSchema(), executionInput);
    }

    private ExecutionResult query(GraphQLSchema schema, ExecutionInput executionInput) {
        return newGraphQL(schema, executionInput.getContext()).execute(executionInput);
    }

    public static Builder newBuilder() {
        return new Builder();
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

        public Builder withDataLoaderDispatcherInstrumentationOptions(Supplier<DataLoaderDispatcherInstrumentationOptions> supplier) {
            this.dataLoaderDispatcherInstrumentationOptionsSupplier = supplier;
            return this;
        }

        public GraphQLQueryInvoker build() {
            return new GraphQLQueryInvoker(getExecutionStrategyProvider, getInstrumentation, getPreparsedDocumentProvider, dataLoaderDispatcherInstrumentationOptionsSupplier);
        }
    }
}
