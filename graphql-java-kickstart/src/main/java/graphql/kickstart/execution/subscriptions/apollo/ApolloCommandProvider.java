package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type;
import java.util.Collection;
import java.util.EnumMap;

public class ApolloCommandProvider {

  private final EnumMap<Type, SubscriptionCommand> commands = new EnumMap<>(Type.class);

  public ApolloCommandProvider(
      GraphQLSubscriptionMapper mapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Collection<ApolloSubscriptionConnectionListener> connectionListeners) {
    commands.put(
        Type.GQL_CONNECTION_INIT, new SubscriptionConnectionInitCommand(connectionListeners));
    commands.put(
        Type.GQL_START,
        new SubscriptionStartCommand(
            mapper, invocationInputFactory, graphQLInvoker, connectionListeners));
    commands.put(Type.GQL_STOP, new SubscriptionStopCommand(connectionListeners));
    commands.put(
        Type.GQL_CONNECTION_TERMINATE,
        new SubscriptionConnectionTerminateCommand(connectionListeners));
  }

  public SubscriptionCommand getByType(Type type) {
    if (commands.containsKey(type)) {
      return commands.get(type);
    }
    throw new IllegalStateException("No command found for type " + type);
  }
}
