package graphql.kickstart.execution;

public class ObjectMapDeserializationException extends RuntimeException {

  ObjectMapDeserializationException(String message) {
    super(message);
  }

  ObjectMapDeserializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
