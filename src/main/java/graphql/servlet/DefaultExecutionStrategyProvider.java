package graphql.servlet;

import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionStrategy;

/**
 * @author Andrew Potter
 */
public class DefaultExecutionStrategyProvider implements ExecutionStrategyProvider {

    private final ExecutionStrategy queryExecutionStrategy;
    private final ExecutionStrategy mutationExecutionStrategy;
    private final ExecutionStrategy subscriptionExecutionStrategy;

    public DefaultExecutionStrategyProvider() {
        this(null);
    }

    public DefaultExecutionStrategyProvider(ExecutionStrategy executionStrategy) {
        this(executionStrategy, null, null);
    }

    public DefaultExecutionStrategyProvider(ExecutionStrategy queryExecutionStrategy, ExecutionStrategy mutationExecutionStrategy, ExecutionStrategy subscriptionExecutionStrategy) {
        this.queryExecutionStrategy = defaultIfNull(queryExecutionStrategy);
        this.mutationExecutionStrategy = defaultIfNull(mutationExecutionStrategy, this.queryExecutionStrategy);
        this.subscriptionExecutionStrategy = defaultIfNull(subscriptionExecutionStrategy, this.queryExecutionStrategy);
    }

    private ExecutionStrategy defaultIfNull(ExecutionStrategy executionStrategy) {
        return defaultIfNull(executionStrategy, new AsyncExecutionStrategy());
    }

    private ExecutionStrategy defaultIfNull(ExecutionStrategy executionStrategy, ExecutionStrategy defaultStrategy) {
        return executionStrategy != null ? executionStrategy : defaultStrategy;
    }

    @Override
    public ExecutionStrategy getQueryExecutionStrategy() {
        return queryExecutionStrategy;
    }

    @Override
    public ExecutionStrategy getMutationExecutionStrategy() {
        return mutationExecutionStrategy;
    }

    @Override
    public ExecutionStrategy getSubscriptionExecutionStrategy() {
        return subscriptionExecutionStrategy;
    }
}
