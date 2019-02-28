package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TestBatchExecutionHandler implements BatchExecutionHandler {

    @Override
    public void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, Writer writer, GraphQLObjectMapper graphQLObjectMapper,
                            BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction) {
        List<ExecutionResult> results = batchedInvocationInput.getExecutionInputs().parallelStream()
            .limit(2)
            .map(input -> queryFunction.apply(batchedInvocationInput, input))
            .collect(Collectors.toList());
        writeResults(results, writer, graphQLObjectMapper);
    }

    private void writeResults(List<ExecutionResult> results, Writer writer, GraphQLObjectMapper mapper) {
        try {
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
