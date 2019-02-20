package graphql.servlet;

import graphql.ExecutionResult;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultGraphQLExecutionResultHandlerFactory implements GraphQLExecutionResultHandlerFactory {
    @Override
    public ExecutionResultHandler getBatchHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
        return new DefaultGraphQLExecutionResultHandler(respWriter, graphQLObjectMapper);
    }

    private class DefaultGraphQLExecutionResultHandler implements ExecutionResultHandler {

        private final List<ExecutionResult> results = new ArrayList<>();

        private final Writer respWriter;

        private final GraphQLObjectMapper graphQLObjectMapper;

        private DefaultGraphQLExecutionResultHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper) {
            this.respWriter = respWriter;
            this.graphQLObjectMapper = graphQLObjectMapper;
        }

        @Override
        public void accept(ExecutionResult result) {
            results.add(result);
        }

        @Override
        public void finalizeResults() {
            try {
                respWriter.write("[");
                Iterator<ExecutionResult> iterator = results.iterator();
                while (iterator.hasNext()) {
                    respWriter.write(graphQLObjectMapper.serializeResultAsJson(iterator.next()));
                    if (iterator.hasNext()) {
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
