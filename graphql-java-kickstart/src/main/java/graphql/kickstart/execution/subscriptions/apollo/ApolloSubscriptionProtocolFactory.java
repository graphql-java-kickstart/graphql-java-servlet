package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionProtocolFactory;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;

/** @author Andrew Potter */
public class ApolloSubscriptionProtocolFactory extends SubscriptionProtocolFactory {

  public static final int KEEP_ALIVE_INTERVAL = 15;
  @Getter private final GraphQLObjectMapper objectMapper;
  private final ApolloCommandProvider commandProvider;
  private KeepAliveSubscriptionConnectionListener keepAlive;

  public ApolloSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker) {
    this(
        objectMapper,
        invocationInputFactory,
        graphQLInvoker,
        Duration.ofSeconds(KEEP_ALIVE_INTERVAL));
  }

  public ApolloSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Duration keepAliveInterval) {
    this(objectMapper, invocationInputFactory, graphQLInvoker, null, keepAliveInterval);
  }

  public ApolloSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Collection<ApolloSubscriptionConnectionListener> connectionListeners) {
    this(
        objectMapper,
        invocationInputFactory,
        graphQLInvoker,
        connectionListeners,
        Duration.ofSeconds(KEEP_ALIVE_INTERVAL));
  }

  public ApolloSubscriptionProtocolFactory(
      GraphQLObjectMapper objectMapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker,
      Collection<ApolloSubscriptionConnectionListener> connectionListeners,
      Duration keepAliveInterval) {
    super("graphql-ws");
    this.objectMapper = objectMapper;
    Set<ApolloSubscriptionConnectionListener> listeners = new HashSet<>();
    if (connectionListeners != null) {
      listeners.addAll(connectionListeners);
    }
    if (keepAliveInterval != null
        && listeners.stream()
            .noneMatch(KeepAliveSubscriptionConnectionListener.class::isInstance)) {
      keepAlive = new KeepAliveSubscriptionConnectionListener(keepAliveInterval);
      listeners.add(keepAlive);
    }
    commandProvider =
        new ApolloCommandProvider(
            new GraphQLSubscriptionMapper(objectMapper),
            invocationInputFactory,
            graphQLInvoker,
            listeners);
  }

  @Override
  public Consumer<String> createConsumer(SubscriptionSession session) {
    return new ApolloSubscriptionConsumer(session, objectMapper, commandProvider);
  }

  @Override
  public void shutdown() {
    if (keepAlive != null) {
      keepAlive.shutdown();
    }
  }
}
