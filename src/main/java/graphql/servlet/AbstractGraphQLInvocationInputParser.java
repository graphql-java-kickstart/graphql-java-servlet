package graphql.servlet;

import graphql.servlet.context.ContextSetting;
import graphql.servlet.core.GraphQLObjectMapper;
import graphql.servlet.input.GraphQLInvocationInputFactory;
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
    return query != null && !query.trim().isEmpty() && query.trim().startsWith("[");
  }

}
