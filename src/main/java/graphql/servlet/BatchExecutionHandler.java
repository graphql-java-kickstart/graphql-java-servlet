package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.io.Writer;
import java.util.function.BiFunction;

/**
 * @author Andrew Potter
 */
public interface BatchExecutionHandler {
    /**
     * Allows separating the logic of handling batch queries from how each individual query is resolved.
     * @param batchedInvocationInput the batch query input
     * @param queryFunction Function to produce query results.
     */
    void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, Writer writer, GraphQLObjectMapper graphQLObjectMapper,
                     BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction);
}

