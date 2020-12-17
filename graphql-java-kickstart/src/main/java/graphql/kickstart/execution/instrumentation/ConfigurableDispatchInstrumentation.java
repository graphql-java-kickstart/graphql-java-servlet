package graphql.kickstart.execution.instrumentation;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetcher;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.stats.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurableDispatchInstrumentation extends DataLoaderDispatcherInstrumentation {

  private static final Logger log = LoggerFactory
      .getLogger(ConfigurableDispatchInstrumentation.class);

  private final DataLoaderDispatcherInstrumentationOptions options;

  private final Function<DataLoaderRegistry, TrackingApproach> approachFunction;

  /**
   * Creates a DataLoaderDispatcherInstrumentation with the default options
   */
  public ConfigurableDispatchInstrumentation(
      Function<DataLoaderRegistry, TrackingApproach> approachFunction) {
    this(DataLoaderDispatcherInstrumentationOptions.newOptions(), approachFunction);
  }

  /**
   * Creates a DataLoaderDispatcherInstrumentation with the specified options
   *
   * @param options the options to control the behaviour
   */
  public ConfigurableDispatchInstrumentation(DataLoaderDispatcherInstrumentationOptions options,
      Function<DataLoaderRegistry, TrackingApproach> approachFunction) {
    this.options = options;
    this.approachFunction = approachFunction;
  }


  @Override
  public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
    DataLoaderRegistry registry = parameters.getExecutionInput().getDataLoaderRegistry();
    return new DataLoaderDispatcherInstrumentationState(registry, approachFunction.apply(registry),
        parameters.getExecutionInput().getExecutionId());
  }

  @Override
  public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher,
      InstrumentationFieldFetchParameters parameters) {
    DataLoaderDispatcherInstrumentationState state = parameters.getInstrumentationState();
    if (state.isAggressivelyBatching()) {
      return dataFetcher;
    }
    //
    // currently only AsyncExecutionStrategy with DataLoader and hence this allows us to "dispatch"
    // on every object if its not using aggressive batching for other execution strategies
    // which allows them to work if used.
    return (DataFetcher<Object>) environment -> {
      Object obj = dataFetcher.get(environment);
      immediatelyDispatch(state);
      return obj;
    };
  }

  private void immediatelyDispatch(DataLoaderDispatcherInstrumentationState state) {
    state.getApproach().dispatch();
  }

  @Override
  public InstrumentationContext<ExecutionResult> beginExecuteOperation(
      InstrumentationExecuteOperationParameters parameters) {
    if (!isDataLoaderCompatibleExecution(parameters.getExecutionContext())) {
      DataLoaderDispatcherInstrumentationState state = parameters.getInstrumentationState();
      state.setAggressivelyBatching(false);
    }
    return new SimpleInstrumentationContext<>();
  }

  private boolean isDataLoaderCompatibleExecution(ExecutionContext executionContext) {
    //
    // currently we only support Query operations and ONLY with AsyncExecutionStrategy as the query ES
    // This may change in the future but this is the fix for now
    //
    if (executionContext.getOperationDefinition().getOperation()
        == OperationDefinition.Operation.QUERY) {
      ExecutionStrategy queryStrategy = executionContext.getQueryStrategy();
      return (queryStrategy instanceof AsyncExecutionStrategy);
    }
    return false;
  }

  @Override
  public ExecutionStrategyInstrumentationContext beginExecutionStrategy(
      InstrumentationExecutionStrategyParameters parameters) {
    DataLoaderDispatcherInstrumentationState state = parameters.getInstrumentationState();
    //
    // if there are no data loaders, there is nothing to do
    //
    if (state.hasNoDataLoaders()) {
      return new ExecutionStrategyInstrumentationContext() {
        @Override
        public void onDispatched(CompletableFuture<ExecutionResult> result) {
        }

        @Override
        public void onCompleted(ExecutionResult result, Throwable t) {
        }
      };

    }
    return state.getApproach().beginExecutionStrategy(parameters);
  }

  @Override
  public InstrumentationContext<Object> beginFieldFetch(
      InstrumentationFieldFetchParameters parameters) {
    DataLoaderDispatcherInstrumentationState state = parameters.getInstrumentationState();
    //
    // if there are no data loaders, there is nothing to do
    //
    if (state.hasNoDataLoaders()) {
      return new SimpleInstrumentationContext<>();
    }
    return state.getApproach().beginFieldFetch(parameters);
  }

  @Override
  public CompletableFuture<ExecutionResult> instrumentExecutionResult(
      ExecutionResult executionResult,
      InstrumentationExecutionParameters parameters) {
    DataLoaderDispatcherInstrumentationState state = parameters.getInstrumentationState();
    state.getApproach().removeTracking(parameters.getExecutionInput().getExecutionId());
    if (!options.isIncludeStatistics()) {
      return CompletableFuture.completedFuture(executionResult);
    } else {
      Map<Object, Object> currentExt = executionResult.getExtensions();
      Map<Object, Object> statsMap = new LinkedHashMap<>(
          currentExt == null ? Collections.emptyMap() : currentExt);
      Map<Object, Object> dataLoaderStats = buildStatsMap(state);
      statsMap.put("dataloader", dataLoaderStats);

      log.debug("Data loader stats : {}", dataLoaderStats);

      return CompletableFuture
          .completedFuture(
              new ExecutionResultImpl(executionResult.getData(), executionResult.getErrors(),
                  statsMap));
    }
  }

  private Map<Object, Object> buildStatsMap(DataLoaderDispatcherInstrumentationState state) {
    DataLoaderRegistry dataLoaderRegistry = state.getDataLoaderRegistry();
    Statistics allStats = dataLoaderRegistry.getStatistics();
    Map<Object, Object> statsMap = new LinkedHashMap<>();
    statsMap.put("overall-statistics", allStats.toMap());

    Map<Object, Object> individualStatsMap = new LinkedHashMap<>();

    for (String dlKey : dataLoaderRegistry.getKeys()) {
      DataLoader<Object, Object> dl = dataLoaderRegistry.getDataLoader(dlKey);
      Statistics statistics = dl.getStatistics();
      individualStatsMap.put(dlKey, statistics.toMap());
    }

    statsMap.put("individual-statistics", individualStatsMap);

    return statsMap;
  }
}
