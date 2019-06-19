package graphql.servlet.input;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NoOpBatchInputPreProcessor implements BatchInputPreProcessor {

    @Override
    public BatchInputPreProcessResult preProcessBatch(GraphQLBatchedInvocationInput batchedInvocationInput, HttpServletRequest request,
                                               HttpServletResponse response) {
        return new BatchInputPreProcessResult(batchedInvocationInput);
    }
}
