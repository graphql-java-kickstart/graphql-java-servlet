package graphql.kickstart.servlet.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** @author Andrew Potter */
public interface GraphQLServletListener {

  /**
   * Called this method when the request started processing.
   * @param request http request
   * @param response http response
   * @return request callback or {@literal null}
   */
  default RequestCallback onRequest(HttpServletRequest request, HttpServletResponse response) {
    return null;
  }

  /**
   * The callback which used to add additional listeners for GraphQL request execution.
   */
  interface RequestCallback {

    /**
     * Called when failed to parse InvocationInput and the response was not written.
     * @param request http request
     * @param response http response
     */
    default void onParseError(
        HttpServletRequest request, HttpServletResponse response, Throwable throwable) {}

    /**
     * Called right before the response will be written and flushed. Can be used for applying some
     * changes to the response object, like adding response headers.
     * @param request http request
     * @param response http response
     */
    default void beforeFlush(HttpServletRequest request, HttpServletResponse response) {}

    /**
     * Called when GraphQL invoked successfully and the response was written already.
     * @param request http request
     * @param response http response
     */
    default void onSuccess(HttpServletRequest request, HttpServletResponse response) {}

    /**
     * Called when GraphQL was failed and the response was written already.
     * @param request http request
     * @param response http response
     */
    default void onError(
        HttpServletRequest request, HttpServletResponse response, Throwable throwable) {}

    /**
     * Called finally once on both success and failed GraphQL invocation. The response is also
     * already written.
     * @param request http request
     * @param response http response
     */
    default void onFinally(HttpServletRequest request, HttpServletResponse response) {}
  }
}
