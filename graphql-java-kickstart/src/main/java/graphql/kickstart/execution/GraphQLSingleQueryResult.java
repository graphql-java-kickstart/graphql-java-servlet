package graphql.kickstart.execution;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class GraphQLSingleQueryResult implements GraphQLQueryResult {

  @Getter private final DecoratedExecutionResult result;

  @Override
  public boolean isBatched() {
    return false;
  }

  @Override
  public boolean isAsynchronous() {
    return result.isAsynchronous();
  }
}
