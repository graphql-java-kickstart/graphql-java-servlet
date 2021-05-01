package graphql.kickstart.execution.subscriptions;

import graphql.kickstart.execution.GraphQLRequest;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;

public interface GraphQLSubscriptionInvocationInputFactory {

  GraphQLSingleInvocationInput create(GraphQLRequest graphQLRequest, SubscriptionSession session);
}
