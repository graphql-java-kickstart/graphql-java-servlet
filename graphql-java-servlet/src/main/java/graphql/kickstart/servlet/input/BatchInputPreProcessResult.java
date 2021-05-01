package graphql.kickstart.servlet.input;

import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;

/**
 * Wraps the result of pre processing a batch. Allows customization of the response code and message
 * if the batch isn't to be executed.
 */
public class BatchInputPreProcessResult {

  private final GraphQLBatchedInvocationInput batchedInvocationInput;

  private final int statusCode;

  private final boolean executable;

  private final String messsage;

  public BatchInputPreProcessResult(GraphQLBatchedInvocationInput graphQLBatchedInvocationInput) {
    this.batchedInvocationInput = graphQLBatchedInvocationInput;
    this.executable = true;
    this.statusCode = 200;
    this.messsage = null;
  }

  public BatchInputPreProcessResult(int statusCode, String messsage) {
    this.batchedInvocationInput = null;
    this.executable = false;
    this.statusCode = statusCode;
    this.messsage = messsage;
  }

  /** @return If the servlet should try executing this batched input */
  public boolean isExecutable() {
    return executable;
  }

  /** @return the batched input the servlet will try to execute. */
  public GraphQLBatchedInvocationInput getBatchedInvocationInput() {
    return batchedInvocationInput;
  }

  /** @return status message the servlet will use if isExecutable is false. */
  public String getStatusMessage() {
    return messsage;
  }

  /** @return status code the servlet will use if if isExecutable is false. */
  public int getStatusCode() {
    return statusCode;
  }
}
