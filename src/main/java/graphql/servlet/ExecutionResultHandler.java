package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author Andrew Potter
 */
public interface ExecutionResultHandler {
    void handleBatch(GraphQLBatchedInvocationInput batchedInvocationInput, BiFunction<GraphQLInvocationInput, ExecutionInput, ExecutionResult> queryFunction);
}

