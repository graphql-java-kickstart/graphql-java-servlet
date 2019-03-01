package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.function.BiFunction;

public class DefaultBatchExecutionHandler implements BatchExecutionHandler {

    @Override
    public void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, HttpServletResponse response, GraphQLObjectMapper graphQLObjectMapper,
                            BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction) {
        response.setContentType(AbstractGraphQLHttpServlet.APPLICATION_JSON_UTF8);
        response.setStatus(AbstractGraphQLHttpServlet.STATUS_OK);
        try {
            Writer writer = response.getWriter();
            Iterator<ExecutionInput> executionInputIterator = batchedInvocationInput.getExecutionInputs().iterator();

            writer.write("[");
            while (executionInputIterator.hasNext()) {
                ExecutionResult result = queryFunction.apply(batchedInvocationInput, executionInputIterator.next());
                writer.write(graphQLObjectMapper.serializeResultAsJson(result));
                if (executionInputIterator.hasNext()) {
                    writer.write(",");
                }
            }
            writer.write("]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

