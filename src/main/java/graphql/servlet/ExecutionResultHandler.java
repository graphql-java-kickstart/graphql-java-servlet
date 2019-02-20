package graphql.servlet;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @author Andrew Potter
 */
public interface ExecutionResultHandler extends Consumer<ExecutionResult> {
    /**
     * Allows the number of queries in a batch to be limited.
     * @param executionInputIterator iterator for the current set of requests.
     * @return if processing should continue.
     */
    default boolean shouldContinue(Iterator<ExecutionInput> executionInputIterator){
        return executionInputIterator.hasNext();
    }

    /**
     * Should perform any actions, such as formatting and sending results, to complete a batch.
     */
    void finalizeResults();
}

