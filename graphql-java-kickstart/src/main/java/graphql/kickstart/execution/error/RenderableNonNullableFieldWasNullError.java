package graphql.kickstart.execution.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.NonNullableFieldWasNullError;
import graphql.language.SourceLocation;
import java.util.List;
import java.util.Map;

class RenderableNonNullableFieldWasNullError implements GraphQLError {

  private final NonNullableFieldWasNullError delegate;

  public RenderableNonNullableFieldWasNullError(
      NonNullableFieldWasNullError nonNullableFieldWasNullError) {
    this.delegate = nonNullableFieldWasNullError;
  }

  @Override
  public String getMessage() {
    return delegate.getMessage();
  }

  @Override
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public List<SourceLocation> getLocations() {
    return delegate.getLocations();
  }

  @Override
  public ErrorType getErrorType() {
    return delegate.getErrorType();
  }

  @Override
  public List<Object> getPath() {
    return delegate.getPath();
  }

  @Override
  public Map<String, Object> toSpecification() {
    return delegate.toSpecification();
  }

  @Override
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public Map<String, Object> getExtensions() {
    return delegate.getExtensions();
  }
}
