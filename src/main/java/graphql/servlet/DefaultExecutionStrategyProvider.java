/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
package graphql.servlet;

import graphql.execution.ExecutionStrategy;
import graphql.execution.SimpleExecutionStrategy;

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
        return defaultIfNull(executionStrategy, new SimpleExecutionStrategy());
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
