package graphql.servlet;

import graphql.GraphQLError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Andrew Potter
 */
public interface GraphQLServletListener {
    default RequestCallback onRequest(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }
    default OperationCallback onOperation(GraphQLContext context, String operationName, String query, Map<String, Object> variables) {
        return null;
    }

    interface RequestCallback {
        default void onSuccess(HttpServletRequest request, HttpServletResponse response) {}
        default void onError(HttpServletRequest request, HttpServletResponse response, Throwable throwable) {}
        default void onFinally(HttpServletRequest request, HttpServletResponse response) {}
    }

    interface OperationCallback {
        default void onSuccess(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data) {}
        default void onError(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data, List<GraphQLError> errors) {}
        default void onFinally(GraphQLContext context, String operationName, String query, Map<String, Object> variables, Object data) {}
    }
}
