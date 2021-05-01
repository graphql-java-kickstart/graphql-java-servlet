package graphql.kickstart.execution.instrumentation;

import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.InstrumentationState;
import java.util.List;
import org.dataloader.DataLoaderRegistry;

/** Dispatching approach that expects to know about all executions before execution starts. */
public class RequestLevelTrackingApproach extends AbstractTrackingApproach {

  public RequestLevelTrackingApproach(
      List<ExecutionId> executionIds, DataLoaderRegistry dataLoaderRegistry) {
    super(dataLoaderRegistry);
    RequestStack requestStack = getStack();
    executionIds.forEach(requestStack::addExecution);
  }

  @Override
  public InstrumentationState createState(ExecutionId executionId) {
    if (!getStack().contains(executionId)) {
      throw new TrackingApproachException(
          String.format("Request tracking not set up with execution id %s", executionId));
    }
    return null;
  }
}
