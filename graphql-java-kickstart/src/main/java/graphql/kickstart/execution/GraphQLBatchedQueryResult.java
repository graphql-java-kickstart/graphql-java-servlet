package graphql.kickstart.execution;

import graphql.ExecutionResult;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class GraphQLBatchedQueryResult implements GraphQLQueryResult {

  @Getter private final List<ExecutionResult> results;

  @Override
  public boolean isBatched() {
    return true;
  }

  @Override
  public boolean isAsynchronous() {
    return false;
  }
}
