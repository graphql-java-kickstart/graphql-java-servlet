package graphql.kickstart.servlet;

public class InvocationInputParseException extends RuntimeException {

  public InvocationInputParseException(Throwable t) {
    super("Request parsing failed", t);
  }
}
