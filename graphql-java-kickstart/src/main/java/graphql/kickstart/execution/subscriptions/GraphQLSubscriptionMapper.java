package graphql.kickstart.execution.subscriptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GraphQLSubscriptionMapper {

  private final GraphQLObjectMapper graphQLObjectMapper;

  public GraphQLRequest readGraphQLRequest(Object payload) {
    return graphQLObjectMapper.getJacksonMapper().convertValue(payload, GraphQLRequest.class);
  }

  public ExecutionResult sanitizeErrors(ExecutionResult executionResult) {
    return graphQLObjectMapper.sanitizeErrors(executionResult);
  }

  public boolean areErrorsPresent(ExecutionResult executionResult) {
    return graphQLObjectMapper.areErrorsPresent(executionResult);
  }

  public Map<String, Object> convertSanitizedExecutionResult(ExecutionResult executionResult) {
    return graphQLObjectMapper.convertSanitizedExecutionResult(executionResult, false);
  }

  public String serialize(Object payload) {
    try {
      return graphQLObjectMapper.getJacksonMapper().writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      return e.getMessage();
    }
  }

}
