package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import javax.servlet.http.HttpServletResponse;
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
     * @param graphQLObjectMapper object mapper used to serialize results
     * @param response http response object
     */
    void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, HttpServletResponse response, GraphQLObjectMapper graphQLObjectMapper,
                     BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction);
}

