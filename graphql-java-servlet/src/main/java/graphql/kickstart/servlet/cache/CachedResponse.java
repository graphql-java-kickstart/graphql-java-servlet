package graphql.kickstart.servlet.cache;

import java.io.Serializable;
import java.util.Objects;

public class CachedResponse implements Serializable {

  private static final long serialVersionUID = 5894555791705575139L;

  private final byte[] contentBytes;

  private final boolean error;
  private final Integer errorStatusCode;
  private final String errorMessage;

  private CachedResponse(
      byte[] contentBytes, boolean error, Integer errorStatusCode, String errorMessage) {
    this.contentBytes = contentBytes;
    this.error = error;
    this.errorStatusCode = errorStatusCode;
    this.errorMessage = errorMessage;
  }

  /**
   * Constructor for success response
   *
   * @param contentBytes bytes array of graphql json response
   */
  public static CachedResponse ofContent(byte[] contentBytes) {
    Objects.requireNonNull(contentBytes, "contentBytes can not be null");

    return new CachedResponse(contentBytes, false, null, null);
  }

  /**
   * Constructor for error response
   *
   * @param errorStatusCode the status code for the error response
   * @param errorMessage the error message for the error response
   */
  public static CachedResponse ofError(int errorStatusCode, String errorMessage) {
    return new CachedResponse(null, true, errorStatusCode, errorMessage);
  }

  /** @return {@literal true} when this request was failed */
  public boolean isError() {
    return error;
  }

  /**
   * @return the response body for success requests, {@literal null} when {@link #isError()} is
   *     {@literal true}
   */
  public byte[] getContentBytes() {
    return contentBytes;
  }

  /** @return the response error code */
  public Integer getErrorStatusCode() {
    return errorStatusCode;
  }

  /** @return the response error message */
  public String getErrorMessage() {
    return errorMessage;
  }
}
