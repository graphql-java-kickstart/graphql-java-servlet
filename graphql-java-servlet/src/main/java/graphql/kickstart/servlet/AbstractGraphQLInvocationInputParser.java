package graphql.kickstart.servlet;

import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
abstract class AbstractGraphQLInvocationInputParser implements GraphQLInvocationInputParser {

  final GraphQLInvocationInputFactory invocationInputFactory;
  final GraphQLObjectMapper graphQLObjectMapper;
  final ContextSetting contextSetting;

  boolean isSingleQuery(String query) {
    return query != null && !query.trim().isEmpty() && !query.trim().startsWith("[");
  }

  boolean isBatchedQuery(String query) {
    return query != null && query.trim().startsWith("[");
  }
}
