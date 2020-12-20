package graphql.kickstart.execution;

public class VariablesDeserializationException extends RuntimeException {

  VariablesDeserializationException(String message) {
    super(message);
  }

  VariablesDeserializationException(String message, Throwable cause) {
    super(message, cause);
  }

}
