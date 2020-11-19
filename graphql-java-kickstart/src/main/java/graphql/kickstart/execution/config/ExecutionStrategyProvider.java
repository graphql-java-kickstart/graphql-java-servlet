package graphql.kickstart.execution.config;

import graphql.execution.ExecutionStrategy;

public interface ExecutionStrategyProvider {

  ExecutionStrategy getQueryExecutionStrategy();

  ExecutionStrategy getMutationExecutionStrategy();

  ExecutionStrategy getSubscriptionExecutionStrategy();
}
