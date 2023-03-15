package graphql.kickstart.servlet;

import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import graphql.kickstart.servlet.input.BatchInputPreProcessor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestBatchInputPreProcessor implements BatchInputPreProcessor {

  public static String BATCH_ERROR_MESSAGE = "Batch limit exceeded";

  @Override
  public BatchInputPreProcessResult preProcessBatch(
      GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response) {
    BatchInputPreProcessResult preProcessResult;
    if (batchedInvocationInput.getExecutionInputs().size() > 2) {
      preProcessResult = new BatchInputPreProcessResult(400, BATCH_ERROR_MESSAGE);
    } else {
      preProcessResult = new BatchInputPreProcessResult(batchedInvocationInput);
    }
    return preProcessResult;
  }
}
