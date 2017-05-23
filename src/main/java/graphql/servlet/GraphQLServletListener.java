/**
 * Copyright 2016 Yurii Rashkovskii
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */
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
