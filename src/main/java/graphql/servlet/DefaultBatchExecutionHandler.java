package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.function.BiFunction;

public class DefaultBatchExecutionHandler implements BatchExecutionHandler {

    @Override
    public void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, Writer writer, GraphQLObjectMapper graphQLObjectMapper,
                            BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction) {
        Iterator<ExecutionInput> executionInputIterator = batchedInvocationInput.getExecutionInputs().iterator();
        try {
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

