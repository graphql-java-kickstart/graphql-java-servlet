package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.function.BiFunction;

public class DefaultGraphQLBatchExecutionHandlerFactory implements GraphQLBatchExecutionHandlerFactory {
    @Override
    public BatchExecutionHandler getBatchHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
        return new DefaultGraphQLBatchExecutionHandler(respWriter, graphQLObjectMapper);
    }

    private class DefaultGraphQLBatchExecutionHandler implements BatchExecutionHandler {

        private final Writer respWriter;

        private final GraphQLObjectMapper graphQLObjectMapper;

        private DefaultGraphQLBatchExecutionHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
            this.respWriter = respWriter;
            this.graphQLObjectMapper = graphQLObjectMapper;
        }

        @Override
        public void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, BiFunction<GraphQLInvocationInput, ExecutionInput,
            ExecutionResult> queryFunction) {
            Iterator<ExecutionInput> executionInputIterator = batchedInvocationInput.getExecutionInputs().iterator();
            try {
                respWriter.write("[");
                while (executionInputIterator.hasNext()) {
                    ExecutionResult result = queryFunction.apply(batchedInvocationInput, executionInputIterator.next());
                    respWriter.write(graphQLObjectMapper.serializeResultAsJson(result));
                    if (executionInputIterator.hasNext()) {
                       respWriter.write(",");
                    }
                }
                respWriter.write("]");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
