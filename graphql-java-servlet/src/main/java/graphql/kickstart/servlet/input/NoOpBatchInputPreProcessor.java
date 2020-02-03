package graphql.kickstart.servlet.input;

import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A default BatchInputPreProcessor that returns the input.
 */
public class NoOpBatchInputPreProcessor implements BatchInputPreProcessor {

    @Override
    public BatchInputPreProcessResult preProcessBatch(GraphQLBatchedInvocationInput batchedInvocationInput, HttpServletRequest request,
                                               HttpServletResponse response) {
        return new BatchInputPreProcessResult(batchedInvocationInput);
    }
}
