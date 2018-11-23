package graphql.servlet.internal;

import graphql.ExecutionResult;

import java.util.function.BiConsumer;

/**
 * @author Andrew Potter
 */
public interface ExecutionResultHandler extends BiConsumer<ExecutionResult, Boolean> {
    @Override
    default void accept(ExecutionResult executionResult, Boolean hasNext) {
        try {
            handle(executionResult, hasNext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void handle(ExecutionResult result, Boolean hasNext) throws Exception;
}

