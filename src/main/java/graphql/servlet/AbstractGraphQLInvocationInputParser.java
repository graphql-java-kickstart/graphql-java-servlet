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
    return !isArrayStart(query);
  }

  private boolean isArrayStart(String s) {
    return s != null && s.trim().startsWith("[");
  }

}
