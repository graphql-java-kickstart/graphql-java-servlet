package graphql.kickstart.execution;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class GraphQLErrorQueryResult implements GraphQLQueryResult {

  private final int statusCode;
  private final String message;

  @Override
  public boolean isBatched() {
    return false;
  }

  @Override
  public boolean isAsynchronous() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }
}
