package graphql.kickstart.servlet.input;

import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface BatchInputPreProcessor {

  /**
   * An injectable object that allows clients to manipulate a batch before executing, or abort
   * altogether.
   *
   * @param batchedInvocationInput the input to process
   * @param request the servlet request
   * @param response the servlet response
   * @return wrapped batch to possibly process.
   */
  BatchInputPreProcessResult preProcessBatch(
      GraphQLBatchedInvocationInput batchedInvocationInput,
      HttpServletRequest request,
      HttpServletResponse response);
}
