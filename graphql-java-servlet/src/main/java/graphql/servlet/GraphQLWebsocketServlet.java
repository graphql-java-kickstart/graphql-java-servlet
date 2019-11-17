package graphql.servlet;

import graphql.servlet.core.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.subscription.SubscriptionConnectionListener;
import graphql.servlet.input.GraphQLInvocationInputFactory;
import graphql.servlet.core.internal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Must be used with {@link #modifyHandshake(ServerEndpointConfig, HandshakeRequest, HandshakeResponse)}
 *
 * @author Andrew Potter
 */
public class GraphQLWebsocketServlet extends Endpoint {

    private static final Logger log = LoggerFactory.getLogger(GraphQLWebsocketServlet.class);

    private static final String HANDSHAKE_REQUEST_KEY = HandshakeRequest.class.getName();
    private static final String PROTOCOL_HANDLER_REQUEST_KEY = SubscriptionProtocolHandler.class.getName();
    private static final CloseReason ERROR_CLOSE_REASON = new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal Server Error");
    private static final CloseReason SHUTDOWN_CLOSE_REASON = new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server Shut Down");

    private final List<SubscriptionProtocolFactory> subscriptionProtocolFactories;
    private final SubscriptionProtocolFactory fallbackSubscriptionProtocolFactory;
    private final List<String> allSubscriptionProtocols;

    private final Map<Session, WsSessionSubscriptions> sessionSubscriptionCache = new ConcurrentHashMap<>();
    private final SubscriptionHandlerInput subscriptionHandlerInput;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean isShutDown = new AtomicBoolean(false);
    private final Object cacheLock = new Object();

    public GraphQLWebsocketServlet(GraphQLQueryInvoker queryInvoker, GraphQLInvocationInputFactory invocationInputFactory, GraphQLObjectMapper graphQLObjectMapper) {
        this(queryInvoker, invocationInputFactory, graphQLObjectMapper, null);
    }

    public GraphQLWebsocketServlet(GraphQLQueryInvoker queryInvoker, GraphQLInvocationInputFactory invocationInputFactory, GraphQLObjectMapper graphQLObjectMapper, SubscriptionConnectionListener subscriptionConnectionListener) {
        this.subscriptionHandlerInput = new SubscriptionHandlerInput(invocationInputFactory, queryInvoker, graphQLObjectMapper, subscriptionConnectionListener);

        subscriptionProtocolFactories = Collections.singletonList(new ApolloSubscriptionProtocolFactory(subscriptionHandlerInput));
        fallbackSubscriptionProtocolFactory = new FallbackSubscriptionProtocolFactory(subscriptionHandlerInput);
        allSubscriptionProtocols = Stream.concat(subscriptionProtocolFactories.stream(), Stream.of(fallbackSubscriptionProtocolFactory))
                                         .map(SubscriptionProtocolFactory::getProtocol)
                                         .collect(Collectors.toList());
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        final WsSessionSubscriptions subscriptions = new WsSessionSubscriptions();
        final HandshakeRequest request = (HandshakeRequest) endpointConfig.getUserProperties().get(HANDSHAKE_REQUEST_KEY);
        final SubscriptionProtocolHandler subscriptionProtocolHandler = (SubscriptionProtocolHandler) endpointConfig.getUserProperties().get(PROTOCOL_HANDLER_REQUEST_KEY);

        synchronized (cacheLock) {
            if (isShuttingDown.get()) {
                throw new IllegalStateException("Server is shutting down!");
            }

            sessionSubscriptionCache.put(session, subscriptions);
        }

        log.debug("Session opened: {}, {}", session.getId(), endpointConfig);

        // This *cannot* be a lambda because of the way undertow checks the class...
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String text) {
                try {
                    subscriptionProtocolHandler.onMessage(request, session, subscriptions, text);
                } catch (Throwable t) {
                    log.error("Error executing websocket query for session: {}", session.getId(), t);
                    closeUnexpectedly(session, t);
                }
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        log.debug("Session closed: {}, {}", session.getId(), closeReason);
        WsSessionSubscriptions subscriptions;
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
            log.warn("Session {} was killed abruptly without calling onClose. Cleaning up session", session.getId());
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

    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        sec.getUserProperties().put(HANDSHAKE_REQUEST_KEY, request);

        List<String> protocol = request.getHeaders().get(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL);
        if (protocol == null) {
            protocol = Collections.emptyList();
        }

        SubscriptionProtocolFactory subscriptionProtocolFactory = getSubscriptionProtocolFactory(protocol);
        sec.getUserProperties().put(PROTOCOL_HANDLER_REQUEST_KEY, subscriptionProtocolFactory.createHandler());

        if (request.getHeaders().get(HandshakeResponse.SEC_WEBSOCKET_ACCEPT) != null) {
            response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, allSubscriptionProtocols);
        }
        if (!protocol.isEmpty()) {
            response.getHeaders().put(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL, Collections.singletonList(subscriptionProtocolFactory.getProtocol()));
        }
    }

    /**
     * Stops accepting connections and closes all existing connections
     */
    public void beginShutDown() {
        synchronized (cacheLock) {
            isShuttingDown.set(true);
            Map<Session, WsSessionSubscriptions> copy = new HashMap<>(sessionSubscriptionCache);

            // Prevent comodification exception since #onClose() is called during session.close(), but we can't necessarily rely on that happening so we close subscriptions here anyway.
            copy.forEach((session, wsSessionSubscriptions) -> {
                wsSessionSubscriptions.close();
                try {
                    session.close(SHUTDOWN_CLOSE_REASON);
                } catch (IOException e) {
                    log.error("Error closing websocket session!", e);
                }
            });

            copy.clear();

            if(!sessionSubscriptionCache.isEmpty()) {
                log.error("GraphQLWebsocketServlet did not shut down cleanly!");
                sessionSubscriptionCache.clear();
            }
        }

        isShutDown.set(true);
    }

    /**
     * @return true when shutdown is complete
     */
    public boolean isShutDown() {
        return isShutDown.get();
    }

    private SubscriptionProtocolFactory getSubscriptionProtocolFactory(List<String> accept) {
        for (String protocol : accept) {
            for (SubscriptionProtocolFactory subscriptionProtocolFactory : subscriptionProtocolFactories) {
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
                .mapToInt(WsSessionSubscriptions::getSubscriptionCount)
                .sum();
    }
}
