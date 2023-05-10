package graphql.kickstart.servlet;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import graphql.kickstart.execution.GraphQLInvoker;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionInvocationInputFactory;
import graphql.kickstart.execution.subscriptions.GraphQLSubscriptionMapper;
import graphql.kickstart.execution.subscriptions.SessionSubscriptions;
import graphql.kickstart.execution.subscriptions.SubscriptionConnectionListener;
import graphql.kickstart.execution.subscriptions.SubscriptionProtocolFactory;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import graphql.kickstart.execution.subscriptions.apollo.ApolloSubscriptionConnectionListener;
import graphql.kickstart.servlet.apollo.ApolloWebSocketSubscriptionProtocolFactory;
import graphql.kickstart.servlet.subscriptions.FallbackSubscriptionProtocolFactory;
import graphql.kickstart.servlet.subscriptions.WebSocketSendSubscriber;
import graphql.kickstart.servlet.subscriptions.WebSocketSubscriptionProtocolFactory;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Must be used with {@link #modifyHandshake(ServerEndpointConfig, HandshakeRequest,
 * HandshakeResponse)}
 *
 * @author Andrew Potter
 */
@Slf4j
public class GraphQLWebsocketServlet extends Endpoint {

  private static final String HANDSHAKE_REQUEST_KEY = HandshakeRequest.class.getName();
  private static final String PROTOCOL_FACTORY_REQUEST_KEY =
      SubscriptionProtocolFactory.class.getName();
  private static final CloseReason ERROR_CLOSE_REASON =
      new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal Server Error");
  private static final CloseReason SHUTDOWN_CLOSE_REASON =
      new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server Shut Down");

  private final List<SubscriptionProtocolFactory> subscriptionProtocolFactories;
  private final SubscriptionProtocolFactory fallbackSubscriptionProtocolFactory;
  private final List<String> allSubscriptionProtocols;

  private final Map<Session, SessionSubscriptions> sessionSubscriptionCache =
      new ConcurrentHashMap<>();
  private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
  private final AtomicBoolean isShutDown = new AtomicBoolean(false);
  private final Object cacheLock = new Object();

  public GraphQLWebsocketServlet(GraphQLConfiguration configuration) {
    this(configuration, null);
  }

  public GraphQLWebsocketServlet(
      GraphQLConfiguration configuration,
      Collection<SubscriptionConnectionListener> connectionListeners) {
    this(
        configuration.getGraphQLInvoker(),
        configuration.getInvocationInputFactory(),
        configuration.getObjectMapper(),
        connectionListeners);
  }

  public GraphQLWebsocketServlet(
      GraphQLInvoker graphQLInvoker,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper) {
    this(graphQLInvoker, invocationInputFactory, graphQLObjectMapper, null);
  }

  public GraphQLWebsocketServlet(
      GraphQLInvoker graphQLInvoker,
      GraphQLSubscriptionInvocationInputFactory invocationInputFactory,
      GraphQLObjectMapper graphQLObjectMapper,
      Collection<SubscriptionConnectionListener> connectionListeners) {
    List<ApolloSubscriptionConnectionListener> listeners = new ArrayList<>();
    if (connectionListeners != null) {
      connectionListeners.stream()
          .filter(ApolloSubscriptionConnectionListener.class::isInstance)
          .map(ApolloSubscriptionConnectionListener.class::cast)
          .forEach(listeners::add);
    }
    subscriptionProtocolFactories =
        singletonList(
            new ApolloWebSocketSubscriptionProtocolFactory(
                graphQLObjectMapper, invocationInputFactory, graphQLInvoker, listeners));
    fallbackSubscriptionProtocolFactory =
        new FallbackSubscriptionProtocolFactory(
            new GraphQLSubscriptionMapper(graphQLObjectMapper),
            invocationInputFactory,
            graphQLInvoker);
    allSubscriptionProtocols =
        Stream.concat(
                subscriptionProtocolFactories.stream(),
                Stream.of(fallbackSubscriptionProtocolFactory))
            .map(SubscriptionProtocolFactory::getProtocol)
            .collect(toList());
  }

  public GraphQLWebsocketServlet(
      List<SubscriptionProtocolFactory> subscriptionProtocolFactory,
      SubscriptionProtocolFactory fallbackSubscriptionProtocolFactory) {

    this.subscriptionProtocolFactories = subscriptionProtocolFactory;
    this.fallbackSubscriptionProtocolFactory = fallbackSubscriptionProtocolFactory;

    allSubscriptionProtocols =
        Stream.concat(
                subscriptionProtocolFactories.stream(),
                Stream.of(fallbackSubscriptionProtocolFactory))
            .map(SubscriptionProtocolFactory::getProtocol)
            .collect(toList());
  }

  @Override
  public void onOpen(Session session, EndpointConfig endpointConfig) {
    final WebSocketSubscriptionProtocolFactory subscriptionProtocolFactory =
        (WebSocketSubscriptionProtocolFactory)
            endpointConfig.getUserProperties().get(PROTOCOL_FACTORY_REQUEST_KEY);

    SubscriptionSession subscriptionSession = subscriptionProtocolFactory.createSession(session);
    synchronized (cacheLock) {
      if (isShuttingDown.get()) {
        throw new IllegalStateException("Server is shutting down!");
      }

      sessionSubscriptionCache.put(session, subscriptionSession.getSubscriptions());
    }

    subscriptionSession.getPublisher().subscribe(new WebSocketSendSubscriber(session));

    log.debug("Session opened: {}, {}", session.getId(), endpointConfig);
    Consumer<String> consumer = subscriptionProtocolFactory.createConsumer(subscriptionSession);

    // This *cannot* be a lambda because of the way undertow checks the class...
    session.addMessageHandler(
        new MessageHandler.Whole<String>() {
          @Override
          public void onMessage(String text) {
            try {
              consumer.accept(text);
            } catch (Exception t) {
              log.error("Error executing websocket query for session: {}", session.getId(), t);
              closeUnexpectedly(session, t);
            }
          }
        });
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    log.debug("Session closed: {}, {}", session.getId(), closeReason);
    SessionSubscriptions subscriptions;
    synchronized (cacheLock) {
      subscriptions = sessionSubscriptionCache.remove(session);
    }
    if (subscriptions != null) {
      subscriptions.close();
    }
  }

  @Override
  public void onError(Session session, Throwable thr) {
    if (thr instanceof EOFException) {
      log.warn(
          "Session {} was killed abruptly without calling onClose. Cleaning up session",
          session.getId());
      onClose(session, ERROR_CLOSE_REASON);
    } else {
      log.error("Error in websocket session: {}", session.getId(), thr);
      closeUnexpectedly(session, thr);
    }
  }

  private void closeUnexpectedly(Session session, Throwable t) {
    try {
      session.close(ERROR_CLOSE_REASON);
    } catch (IOException e) {
      log.error("Error closing websocket session for session: {}", session.getId(), t);
    }
  }

  public void modifyHandshake(
      ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
    sec.getUserProperties().put(HANDSHAKE_REQUEST_KEY, request);

    List<String> protocol = request.getHeaders().get(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL);
    if (protocol == null) {
      protocol = Collections.emptyList();
    }

    SubscriptionProtocolFactory subscriptionProtocolFactory =
        getSubscriptionProtocolFactory(protocol);
    sec.getUserProperties().put(PROTOCOL_FACTORY_REQUEST_KEY, subscriptionProtocolFactory);

    if (request.getHeaders().get(HandshakeResponse.SEC_WEBSOCKET_ACCEPT) != null) {
      response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, allSubscriptionProtocols);
    }
    if (!protocol.isEmpty()) {
      //noinspection ArraysAsListWithZeroOrOneArgument
      response
          .getHeaders()
          .put(
              HandshakeRequest.SEC_WEBSOCKET_PROTOCOL,
              new ArrayList<>(asList(subscriptionProtocolFactory.getProtocol())));
    }
  }

  /** Stops accepting connections and closes all existing connections */
  public void beginShutDown() {
    synchronized (cacheLock) {
      isShuttingDown.set(true);
      Map<Session, SessionSubscriptions> copy = new HashMap<>(sessionSubscriptionCache);

      // Prevent comodification exception since #onClose() is called during session.close(), but we
      // can't necessarily rely on that happening so we close subscriptions here anyway.
      copy.forEach(
          (session, wsSessionSubscriptions) -> {
            wsSessionSubscriptions.close();
            try {
              session.close(SHUTDOWN_CLOSE_REASON);
            } catch (IOException e) {
              log.error("Error closing websocket session!", e);
            }
          });

      copy.clear();

      if (!sessionSubscriptionCache.isEmpty()) {
        log.error("GraphQLWebsocketServlet did not shut down cleanly!");
        sessionSubscriptionCache.clear();
      }

      for (SubscriptionProtocolFactory protocolFactory : subscriptionProtocolFactories) {
        protocolFactory.shutdown();
      }

      fallbackSubscriptionProtocolFactory.shutdown();
    }

    isShutDown.set(true);
  }

  /** @return true when shutdown is complete */
  public boolean isShutDown() {
    return isShutDown.get();
  }

  private SubscriptionProtocolFactory getSubscriptionProtocolFactory(List<String> accept) {
    for (String protocol : accept) {
      for (SubscriptionProtocolFactory subscriptionProtocolFactory :
          subscriptionProtocolFactories) {
        if (subscriptionProtocolFactory.getProtocol().equals(protocol)) {
          return subscriptionProtocolFactory;
        }
      }
    }

    return fallbackSubscriptionProtocolFactory;
  }

  public int getSessionCount() {
    return sessionSubscriptionCache.size();
  }

  public int getSubscriptionCount() {
    return sessionSubscriptionCache.values().stream()
        .mapToInt(SessionSubscriptions::getSubscriptionCount)
        .sum();
  }
}
