package graphql.kickstart.execution.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.ExecutionId;
import graphql.execution.FieldValueInfo;
import graphql.execution.ResultPath;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoaderRegistry;

/** Handles logic common to tracking approaches. */
@Slf4j
public abstract class AbstractTrackingApproach implements TrackingApproach {

  private final DataLoaderRegistry dataLoaderRegistry;

  private final RequestStack stack = new RequestStack();

  protected AbstractTrackingApproach(DataLoaderRegistry dataLoaderRegistry) {
    this.dataLoaderRegistry = dataLoaderRegistry;
  }

  /** @return allows extending classes to modify the stack. */
  protected RequestStack getStack() {
    return stack;
  }

  @Override
  public ExecutionStrategyInstrumentationContext beginExecutionStrategy(
      InstrumentationExecutionStrategyParameters parameters) {
    ExecutionId executionId = parameters.getExecutionContext().getExecutionId();
    ResultPath path = parameters.getExecutionStrategyParameters().getPath();
    int parentLevel = path.getLevel();
    int curLevel = parentLevel + 1;
    int fieldCount = parameters.getExecutionStrategyParameters().getFields().size();
    synchronized (stack) {
      stack.increaseExpectedFetchCount(executionId, curLevel, fieldCount);
      stack.increaseHappenedStrategyCalls(executionId, curLevel);
    }

    return new ExecutionStrategyInstrumentationContext() {
      @Override
      public void onDispatched(CompletableFuture<ExecutionResult> result) {
        // default empty implementation
      }

      @Override
      public void onCompleted(ExecutionResult result, Throwable t) {
        // default empty implementation
      }

      @Override
      public void onFieldValuesInfo(List<FieldValueInfo> fieldValueInfoList) {
        synchronized (stack) {
          stack.setStatus(
              executionId,
              handleOnFieldValuesInfo(fieldValueInfoList, stack, executionId, curLevel));
          if (stack.allReady()) {
            dispatchWithoutLocking();
          }
        }
      }
    };
  }

  //
  // thread safety : called with synchronised(stack)
  //
  private boolean handleOnFieldValuesInfo(
      List<FieldValueInfo> fieldValueInfoList,
      RequestStack stack,
      ExecutionId executionId,
      int curLevel) {
    stack.increaseHappenedOnFieldValueCalls(executionId, curLevel);
    int expectedStrategyCalls = 0;
    for (FieldValueInfo fieldValueInfo : fieldValueInfoList) {
      if (fieldValueInfo.getCompleteValueType() == FieldValueInfo.CompleteValueType.OBJECT) {
        expectedStrategyCalls++;
      } else if (fieldValueInfo.getCompleteValueType() == FieldValueInfo.CompleteValueType.LIST) {
        expectedStrategyCalls += getCountForList(fieldValueInfo);
      }
    }
    stack.increaseExpectedStrategyCalls(executionId, curLevel + 1, expectedStrategyCalls);
    return dispatchIfNeeded(stack, executionId, curLevel + 1);
  }

  private int getCountForList(FieldValueInfo fieldValueInfo) {
    int result = 0;
    for (FieldValueInfo cvi : fieldValueInfo.getFieldValueInfos()) {
      if (cvi.getCompleteValueType() == FieldValueInfo.CompleteValueType.OBJECT) {
        result++;
      } else if (cvi.getCompleteValueType() == FieldValueInfo.CompleteValueType.LIST) {
        result += getCountForList(cvi);
      }
    }
    return result;
  }

  @Override
  public InstrumentationContext<Object> beginFieldFetch(
      InstrumentationFieldFetchParameters parameters) {
    ExecutionId executionId = parameters.getExecutionContext().getExecutionId();
    ResultPath path = parameters.getEnvironment().getExecutionStepInfo().getPath();
    int level = path.getLevel();
    return new InstrumentationContext<Object>() {

      @Override
      public void onDispatched(CompletableFuture result) {
        synchronized (stack) {
          stack.increaseFetchCount(executionId, level);
          stack.setStatus(executionId, dispatchIfNeeded(stack, executionId, level));

          if (stack.allReady()) {
            dispatchWithoutLocking();
          }
        }
      }

      @Override
      public void onCompleted(Object result, Throwable t) {
        // default empty implementation
      }
    };
  }

  @Override
  public void removeTracking(ExecutionId executionId) {
    synchronized (stack) {
      stack.removeExecution(executionId);
      if (stack.allReady()) {
        dispatchWithoutLocking();
      }
    }
  }

  //
  // thread safety : called with synchronised(stack)
  //
  private boolean dispatchIfNeeded(RequestStack stack, ExecutionId executionId, int level) {
    if (levelReady(stack, executionId, level)) {
      return stack.dispatchIfNotDispatchedBefore(executionId, level);
    }
    return false;
  }

  //
  // thread safety : called with synchronised(stack)
  //
  private boolean levelReady(RequestStack stack, ExecutionId executionId, int level) {
    if (level == 1) {
      // level 1 is special: there is only one strategy call and that's it
      return stack.allFetchesHappened(executionId, 1);
    }
    return (levelReady(stack, executionId, level - 1)
        && stack.allOnFieldCallsHappened(executionId, level - 1)
        && stack.allStrategyCallsHappened(executionId, level)
        && stack.allFetchesHappened(executionId, level));
  }

  @Override
  public void dispatch() {
    synchronized (stack) {
      dispatchWithoutLocking();
    }
  }

  private void dispatchWithoutLocking() {
    log.debug("Dispatching data loaders ({})", dataLoaderRegistry.getKeys());
    dataLoaderRegistry.dispatchAll();
    stack.allReset();
  }
}
