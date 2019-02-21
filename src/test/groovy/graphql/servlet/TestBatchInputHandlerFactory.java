package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TestBatchInputHandlerFactory implements GraphQLExecutionResultHandlerFactory {
    @Override
    public ExecutionResultHandler getBatchHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
        return new LimitedBatchSizeHandler(respWriter, graphQLObjectMapper);
    }

    private class LimitedBatchSizeHandler implements ExecutionResultHandler {

        Writer writer;
        GraphQLObjectMapper mapper;

        private LimitedBatchSizeHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
            this.mapper = graphQLObjectMapper;
            this.writer = respWriter;
        }

        @Override
        public void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, BiFunction<GraphQLInvocationInput, ExecutionInput,
            ExecutionResult> queryFunction) {
            List<ExecutionResult> results = batchedInvocationInput.getExecutionInputs().parallelStream()
                .limit(2)
                .map(input -> queryFunction.apply(batchedInvocationInput, input))
                .collect(Collectors.toList());
            writeResults(results);
        }

        public void writeResults(List<ExecutionResult> results) {
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
}
