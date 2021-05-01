package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.DefaultSubscriptionSession;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApolloSubscriptionSession extends DefaultSubscriptionSession {

  public ApolloSubscriptionSession(GraphQLSubscriptionMapper mapper) {
    super(mapper);
  }

  @Override
  public void sendDataMessage(String id, Object payload) {
    sendMessage(new OperationMessage(Type.GQL_DATA, id, payload));
  }

  @Override
  public void sendErrorMessage(String id, Object payload) {
    sendMessage(new OperationMessage(Type.GQL_ERROR, id, payload));
  }

  @Override
  public void sendCompleteMessage(String id) {
    sendMessage(new OperationMessage(Type.GQL_COMPLETE, id, null));
  }
}
