package graphql.servlet;

import static graphql.servlet.HttpRequestHandler.APPLICATION_GRAPHQL;
import static java.util.stream.Collectors.joining;

import graphql.GraphQLException;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.core.internal.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class GraphQLPostInvocationInputParser extends AbstractGraphQLInvocationInputParser {

  GraphQLPostInvocationInputParser(GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper, ContextSetting contextSetting) {
    super(invocationInputFactory, graphQLObjectMapper, contextSetting);
  }

  public GraphQLInvocationInput getGraphQLInvocationInput(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (APPLICATION_GRAPHQL.equals(request.getContentType())) {
      String query = request.getReader().lines().collect(joining());
      GraphQLRequest graphqlRequest = GraphQLRequest.createQueryOnlyRequest(query);
      return invocationInputFactory.create(graphqlRequest, request, response);
    }

    String body = request.getReader().lines().collect(joining());
    if (isSingleQuery(body)) {
      GraphQLRequest graphqlRequest = graphQLObjectMapper.readGraphQLRequest(body);
      return invocationInputFactory.create(graphqlRequest, request, response);
    }

    if (isBatchedQuery(body)) {
      List<GraphQLRequest> requests = graphQLObjectMapper.readBatchedGraphQLRequest(body);
      return invocationInputFactory.create(contextSetting, requests, request, response);
    }

    throw new GraphQLException("No valid query found in request");
  }

}
