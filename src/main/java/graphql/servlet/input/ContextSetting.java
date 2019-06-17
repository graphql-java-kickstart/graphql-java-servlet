package graphql.servlet.input;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLContext;
import graphql.servlet.instrumentation.ConfigurableDispatchInstrumentation;
import graphql.servlet.instrumentation.FieldLevelTrackingApproach;
import graphql.servlet.instrumentation.RequestLevelTrackingApproach;
import graphql.servlet.internal.GraphQLRequest;
import org.dataloader.DataLoaderRegistry;

import java.util.Arrays;
import java.util.List;

import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum ContextSetting {

    PER_REQUEST,
    PER_QUERY;

    public GraphQLBatchedInvocationInput getBatch(List<GraphQLRequest> requests, GraphQLSchema schema, Supplier<GraphQLContext> contextSupplier, Object root) {
        switch (this) {
            case PER_QUERY:
                return new PerQueryBatchedInvocationInput(requests, schema, contextSupplier, root);
            case PER_REQUEST:
                return new PerRequestBatchedInvocationInput(requests, schema, contextSupplier.get(), root);
                default:
                    throw new RuntimeException("Unconfigured context setting type");
        }
    }

    public Supplier<Instrumentation> configureInstrumentationForContext(Supplier<Instrumentation> instrumentation, List<ExecutionInput> executionInputs,
                                                                        DataLoaderDispatcherInstrumentationOptions options) {
        ConfigurableDispatchInstrumentation dispatchInstrumentation;
        switch (this) {
            case PER_REQUEST:
                DataLoaderRegistry registry = executionInputs.stream().findFirst().map(ExecutionInput::getDataLoaderRegistry)
                    .orElseThrow(IllegalArgumentException::new);
                List<ExecutionId> executionIds = executionInputs.stream().map(ExecutionInput::getExecutionId).collect(Collectors.toList());
                RequestLevelTrackingApproach requestTrackingApproach = new RequestLevelTrackingApproach(executionIds, registry);
                dispatchInstrumentation = new ConfigurableDispatchInstrumentation(options,
                    (dataLoaderRegistry -> requestTrackingApproach));
                break;
            case PER_QUERY:
                dispatchInstrumentation = new ConfigurableDispatchInstrumentation(options,
                    (dataLoaderRegistry) -> new FieldLevelTrackingApproach(dataLoaderRegistry));
                break;
                default:
                    throw new RuntimeException("Unconfigured context setting type");
        }
        return () -> new ChainedInstrumentation(Arrays.asList(dispatchInstrumentation, instrumentation.get()));
    }
}
