package graphql.kickstart.servlet;

import graphql.GraphQLException;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GraphQLGetInvocationInputParser extends AbstractGraphQLInvocationInputParser {

  GraphQLGetInvocationInputParser(
      GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper,
      ContextSetting contextSetting) {
    super(invocationInputFactory, graphQLObjectMapper, contextSetting);
  }

  public GraphQLInvocationInput getGraphQLInvocationInput(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (isIntrospectionQuery(request)) {
      GraphQLRequest graphqlRequest = GraphQLRequest.createIntrospectionRequest();
      return invocationInputFactory.create(graphqlRequest, request, response);
    }

    String query = request.getParameter("query");
    if (query == null) {
      throw new GraphQLException("Query parameter not found in GET request");
    }

    if (isSingleQuery(query)) {
      Map<String, Object> variables = getVariables(request);
      Map<String, Object> extensions = getExtensions(request);
      String operationName = request.getParameter("operationName");
      GraphQLRequest graphqlRequest =
          new GraphQLRequest(query, variables, extensions, operationName);
      return invocationInputFactory.createReadOnly(graphqlRequest, request, response);
    }

    List<GraphQLRequest> graphqlRequests = graphQLObjectMapper.readBatchedGraphQLRequest(query);
    return invocationInputFactory.createReadOnly(
        contextSetting, graphqlRequests, request, response);
  }

  private boolean isIntrospectionQuery(HttpServletRequest request) {
    String path =
        Optional.ofNullable(request.getPathInfo()).orElseGet(request::getServletPath).toLowerCase();
    return path.contentEquals("/schema.json");
  }

  private Map<String, Object> getVariables(HttpServletRequest request) {
    return Optional.ofNullable(request.getParameter("variables"))
        .map(graphQLObjectMapper::deserializeVariables)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
  }

  private Map<String, Object> getExtensions(HttpServletRequest request) {
    return Optional.ofNullable(request.getParameter("extensions"))
        .map(graphQLObjectMapper::deserializeExtensions)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
  }
}
