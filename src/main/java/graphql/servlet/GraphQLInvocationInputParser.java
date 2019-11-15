package graphql.servlet;

import graphql.servlet.context.ContextSetting;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.core.internal.GraphQLRequest;
import graphql.servlet.input.GraphQLInvocationInput;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class GraphQLInvocationInputParser {

  private final GraphQLInvocationInputFactory invocationInputFactory;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final ContextSetting contextSetting;

  GraphQLInvocationInput getGraphQLInvocationInput(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (isIntrospectionQuery(request)) {
      GraphQLRequest graphqlRequest = GraphQLRequest.createIntrospectionRequest();
      return invocationInputFactory.create(graphqlRequest, request, response);
    }

    String query = request.getParameter("query");
    if (!isBatchedQuery(query)) {
      Map<String, Object> variables = getVariables(request);
      String operationName = request.getParameter("operationName");
      GraphQLRequest graphqlRequest = new GraphQLRequest(query, variables, operationName);
      return invocationInputFactory.create(graphqlRequest, request, response);
    }

    List<GraphQLRequest> requests = graphQLObjectMapper.readBatchedGraphQLRequest(query);
    return invocationInputFactory.createReadOnly(contextSetting, requests, request, response);
  }

  private boolean isIntrospectionQuery(HttpServletRequest request) {
    String path = Optional.ofNullable(request.getPathInfo()).orElseGet(request::getServletPath).toLowerCase();
    return path.contentEquals("/schema.json");
  }

  private Map<String, Object> getVariables(HttpServletRequest request) {
    return Optional.ofNullable(request.getParameter("variables"))
        .map(graphQLObjectMapper::deserializeVariables)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
  }

  private boolean isBatchedQuery(String query) {
    return isArrayStart(query);
  }

  private boolean isArrayStart(String s) {
    return s != null && s.trim().startsWith("[");
  }

}
