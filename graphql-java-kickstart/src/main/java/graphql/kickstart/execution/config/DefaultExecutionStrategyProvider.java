package graphql.kickstart.execution.config;

import graphql.execution.ExecutionStrategy;

/** @author Andrew Potter */
public class DefaultExecutionStrategyProvider implements ExecutionStrategyProvider {

  private final ExecutionStrategy queryExecutionStrategy;
  private final ExecutionStrategy mutationExecutionStrategy;
  private final ExecutionStrategy subscriptionExecutionStrategy;

  public DefaultExecutionStrategyProvider() {
    this(null);
  }

  public DefaultExecutionStrategyProvider(ExecutionStrategy executionStrategy) {
    this(executionStrategy, executionStrategy, null);
  }

  public DefaultExecutionStrategyProvider(
      ExecutionStrategy queryExecutionStrategy,
      ExecutionStrategy mutationExecutionStrategy,
      ExecutionStrategy subscriptionExecutionStrategy) {
    this.queryExecutionStrategy = queryExecutionStrategy;
    this.mutationExecutionStrategy = mutationExecutionStrategy;
    this.subscriptionExecutionStrategy = subscriptionExecutionStrategy;
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
