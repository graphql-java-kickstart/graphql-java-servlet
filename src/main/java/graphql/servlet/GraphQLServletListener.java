package graphql.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Andrew Potter
 */
public interface GraphQLServletListener {
    default void onStart(HttpServletRequest request, HttpServletResponse response) {}
    default void onError(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {}
    default void onFinally(HttpServletRequest request, HttpServletResponse response) {}
}
