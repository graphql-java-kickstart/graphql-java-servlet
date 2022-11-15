package graphql.kickstart.servlet;

import static java.util.stream.Collectors.joining;

import graphql.GraphQLException;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class GraphQLPostInvocationInputParser extends AbstractGraphQLInvocationInputParser {

  private static final String APPLICATION_GRAPHQL = "application/graphql";

  GraphQLPostInvocationInputParser(
      GraphQLInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper,
      ContextSetting contextSetting) {
    super(invocationInputFactory, graphQLObjectMapper, contextSetting);
  }

  public GraphQLInvocationInput getGraphQLInvocationInput(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    String contentType = request.getContentType();
    if (contentType != null && APPLICATION_GRAPHQL.equals(contentType.split(";")[0].trim())) {
      String query = request.getReader().lines().collect(joining(" "));
      GraphQLRequest graphqlRequest = GraphQLRequest.createQueryOnlyRequest(query);
      return invocationInputFactory.create(graphqlRequest, request, response);
    }

    String body = request.getReader().lines().collect(joining(" "));
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
