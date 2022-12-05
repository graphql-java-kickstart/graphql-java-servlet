package graphql.kickstart.servlet.apollo;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import graphql.kickstart.execution.subscriptions.apollo.ApolloSubscriptionConnectionListener;
import graphql.kickstart.execution.subscriptions.apollo.ApolloSubscriptionProtocolFactory;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionProtocolFactory;
import java.time.Duration;
import java.util.Collection;
import jakarta.websocket.Session;

public class ApolloWebSocketSubscriptionProtocolFactory extends ApolloSubscriptionProtocolFactory
    implements WebSocketSubscriptionProtocolFactory {

  public ApolloWebSocketSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker) {
    super(objectMapper, invocationInputFactory, graphQLInvoker);
  }

  public ApolloWebSocketSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Duration keepAliveInterval) {
    super(objectMapper, invocationInputFactory, graphQLInvoker, keepAliveInterval);
  }

  public ApolloWebSocketSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Collection<ApolloSubscriptionConnectionListener> connectionListeners) {
    super(objectMapper, invocationInputFactory, graphQLInvoker, connectionListeners);
  }

  public ApolloWebSocketSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Collection<ApolloSubscriptionConnectionListener> connectionListeners,
      Duration keepAliveInterval) {
    super(
        objectMapper,
        invocationInputFactory,
        graphQLInvoker,
        connectionListeners,
        keepAliveInterval);
  }

  @Override
  public SubscriptionSession createSession(Session session) {
    return new ApolloWebSocketSubscriptionSession(
        new GraphQLSubscriptionMapper(getObjectMapper()), session);
  }
}
