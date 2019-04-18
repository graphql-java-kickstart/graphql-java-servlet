package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TestBatchExecutionHandler implements BatchExecutionHandler {

    public static String BATCH_ERROR_MESSAGE = "Batch limit exceeded";

    @Override
    public void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, HttpServletResponse response, GraphQLObjectMapper graphQLObjectMapper,
                            BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction) {
        List<ExecutionInput> inputs = batchedInvocationInput.getExecutionInputs();
        if (inputs.size() > 2) {
            handleBadInput(response);
        }
        List<ExecutionResult> results = inputs.parallelStream()
            .map(input -> queryFunction.apply(batchedInvocationInput, input))
            .collect(Collectors.toList());
        writeResults(results, response, graphQLObjectMapper);
    }

    private void handleBadInput(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, BATCH_ERROR_MESSAGE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeResults(List<ExecutionResult> results, HttpServletResponse response, GraphQLObjectMapper mapper) {
        response.setContentType(AbstractGraphQLHttpServlet.APPLICATION_JSON_UTF8);
        response.setStatus(AbstractGraphQLHttpServlet.STATUS_OK);
        try {
            Writer writer = response.getWriter();
            writer.write("[");
            Iterator<ExecutionResult> iter = results.iterator();
            while (iter.hasNext()) {
                writer.write(mapper.serializeResultAsJson(iter.next()));
                if (iter.hasNext()) {
                    writer.write(",");
                }
            }
            writer.write("]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
