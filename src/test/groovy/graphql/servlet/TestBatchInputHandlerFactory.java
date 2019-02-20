package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestBatchInputHandlerFactory implements GraphQLExecutionResultHandlerFactory {
    @Override
    public ExecutionResultHandler getBatchHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
        return new LimitedBatchSizeHandler(respWriter, graphQLObjectMapper);
    }

    private class LimitedBatchSizeHandler implements ExecutionResultHandler {

        Writer writer;
        GraphQLObjectMapper mapper;
        List<ExecutionResult> results = new ArrayList<>();

        private LimitedBatchSizeHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
            this.mapper = graphQLObjectMapper;
            this.writer = respWriter;
        }

        @Override
        public void finalizeResults() {
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

        @Override
        public void accept(ExecutionResult executionResult) {
            results.add(executionResult);
        }

        @Override
        public boolean shouldContinue(Iterator<ExecutionInput> executionInputIterator) {
            return results.size() < 2 && executionInputIterator.hasNext();
        }
    }
}
