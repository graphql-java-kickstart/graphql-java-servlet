package graphql.kickstart.execution.error;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.List;

/** @author Andrew Potter */
public class GenericGraphQLError implements GraphQLError {

  private final String message;

  public GenericGraphQLError(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  @JsonIgnore
  public List<SourceLocation> getLocations() {
    return emptyList();
  }

  @Override
  @JsonIgnore
  public ErrorType getErrorType() {
    return null;
  }
}
