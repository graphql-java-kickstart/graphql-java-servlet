package graphql.servlet;

import static java.util.Collections.emptyList;

import graphql.ExecutionResult;
import java.util.List;

public interface GraphQLQueryResult {

  static GraphQLSingleQueryResult create(ExecutionResult result) {
    return new GraphQLSingleQueryResult(new DecoratedExecutionResult(result));
  }

  static GraphQLBatchedQueryResult create(List<ExecutionResult> results) {
    return new GraphQLBatchedQueryResult(results);
  }

  boolean isBatched();

  boolean isAsynchronous();

  default DecoratedExecutionResult getResult() {
    return null;
  }

  default List<ExecutionResult> getResults() {
    return emptyList();
  }

  default boolean isError() { return false; }
}
