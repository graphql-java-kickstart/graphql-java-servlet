package graphql.servlet;

import graphql.servlet.input.BatchInputPreProcessResult;
import graphql.servlet.input.BatchInputPreProcessor;
import graphql.servlet.input.GraphQLBatchedInvocationInput;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TestBatchExecutionHandler implements BatchInputPreProcessor {

    public static String BATCH_ERROR_MESSAGE = "Batch limit exceeded";

    @Override
    public BatchInputPreProcessResult preProcessBatch(GraphQLBatchedInvocationInput batchedInvocationInput, HttpServletRequest request,
                                                      HttpServletResponse response) {
        BatchInputPreProcessResult preProcessResult;
        if (batchedInvocationInput.getExecutionInputs().size() > 2) {
            handleBadInput(response);
            preProcessResult = new BatchInputPreProcessResult(null, false);
        } else {
            preProcessResult = new BatchInputPreProcessResult(batchedInvocationInput, true);
        }
        return preProcessResult;
    }

    private void handleBadInput(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, BATCH_ERROR_MESSAGE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
