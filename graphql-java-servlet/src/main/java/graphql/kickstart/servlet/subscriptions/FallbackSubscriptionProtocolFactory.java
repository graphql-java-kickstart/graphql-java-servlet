package graphql.kickstart.servlet.subscriptions;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionProtocolFactory;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.util.function.Consumer;
import jakarta.websocket.Session;

/** @author Andrew Potter */
public class FallbackSubscriptionProtocolFactory extends SubscriptionProtocolFactory
    implements WebSocketSubscriptionProtocolFactory {

  private final GraphQLSubscriptionMapper mapper;
  private final GraphQLSubscriptionInvocationInputFactory invocationInputFactory;
  private final GraphQLInvoker graphQLInvoker;

  public FallbackSubscriptionProtocolFactory(
      GraphQLSubscriptionMapper mapper,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLInvoker graphQLInvoker) {
    super("");
    this.mapper = mapper;
    this.invocationInputFactory = invocationInputFactory;
    this.graphQLInvoker = graphQLInvoker;
  }

  @Override
  public Consumer<String> createConsumer(SubscriptionSession session) {
    return new FallbackSubscriptionConsumer(
        session, mapper, invocationInputFactory, graphQLInvoker);
  }

  @Override
  public SubscriptionSession createSession(Session session) {
    return new WebSocketSubscriptionSession(mapper, session);
  }
}
