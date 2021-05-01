package graphql.kickstart.execution;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
class DecoratedExecutionResult implements ExecutionResult {

  private final ExecutionResult result;

  boolean isAsynchronous() {
    return result.getData() instanceof Publisher;
  }

  @Override
  public List<GraphQLError> getErrors() {
    return result.getErrors();
  }

  @Override
  public <T> T getData() {
    return result.getData();
  }

  @Override
  public boolean isDataPresent() {
    return result.isDataPresent();
  }

  @Override
  public Map<Object, Object> getExtensions() {
    return result.getExtensions();
  }

  @Override
  public Map<String, Object> toSpecification() {
    return result.toSpecification();
  }
}
