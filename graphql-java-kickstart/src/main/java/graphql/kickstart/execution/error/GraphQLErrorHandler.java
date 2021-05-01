package graphql.kickstart.execution.error;

import graphql.GraphQLError;
import java.util.List;

/** @author Andrew Potter */
public interface GraphQLErrorHandler {

  default boolean errorsPresent(List<GraphQLError> errors) {
    return errors != null && !errors.isEmpty();
  }

  List<GraphQLError> processErrors(List<GraphQLError> errors);
}
