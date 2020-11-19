package graphql.kickstart.execution;

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

  static GraphQLErrorQueryResult createError(int statusCode, String message) {
    return new GraphQLErrorQueryResult(statusCode, message);
  }

  boolean isBatched();

  boolean isAsynchronous();

  default DecoratedExecutionResult getResult() {
    return null;
  }

  default List<ExecutionResult> getResults() {
    return emptyList();
  }

  default boolean isError() {
    return false;
  }

  default int getStatusCode() {
    return 200;
  }

  default String getMessage() {
    return null;
  }
}
