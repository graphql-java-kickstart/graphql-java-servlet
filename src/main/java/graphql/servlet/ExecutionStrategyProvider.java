package graphql.servlet;

import graphql.execution.ExecutionStrategy;

public interface ExecutionStrategyProvider {
    ExecutionStrategy getQueryExecutionStrategy();
    ExecutionStrategy getMutationExecutionStrategy();
    ExecutionStrategy getSubscriptionExecutionStrategy();
}
