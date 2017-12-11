package graphql.servlet;

import graphql.servlet.internal.ApolloSubscriptionProtocolFactory;
import graphql.servlet.internal.FallbackSubscriptionProtocolFactory;
import graphql.servlet.internal.SubscriptionProtocolFactory;
import graphql.servlet.internal.SubscriptionProtocolHandler;
import graphql.servlet.internal.WsSessionSubscriptions;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.servlet.AbstractGraphQLHttpServlet.log;

/**
 * Must be used with {@link #modifyHandshake(ServerEndpointConfig, HandshakeRequest, HandshakeResponse)}
 *
 * @author Andrew Potter
 */
public class GraphQLWebsocketServlet extends Endpoint {

    private static final String HANDSHAKE_REQUEST_KEY = HandshakeRequest.class.getName();
    private static final String PROTOCOL_HANDLER_REQUEST_KEY = SubscriptionProtocolHandler.class.getName();
    private static final CloseReason ERROR_CLOSE_REASON = new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal Server Error");

    private static final List<SubscriptionProtocolFactory> subscriptionProtocolFactories = Collections.singletonList(new ApolloSubscriptionProtocolFactory());
    private static final SubscriptionProtocolFactory fallbackSubscriptionProtocolFactory = new FallbackSubscriptionProtocolFactory();
    private static final List<String> allSubscriptionProtocols;

    static {
        allSubscriptionProtocols = Stream.concat(subscriptionProtocolFactories.stream(), Stream.of(fallbackSubscriptionProtocolFactory))
            .map(SubscriptionProtocolFactory::getProtocol)
            .collect(Collectors.toList());
    }

    private final Map<Session, WsSessionSubscriptions> sessionSubscriptionCache = new HashMap<>();
    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final GraphQLObjectMapper graphQLObjectMapper;

    public GraphQLWebsocketServlet(GraphQLQueryInvoker queryInvoker, GraphQLInvocationInputFactory invocationInputFactory, GraphQLObjectMapper graphQLObjectMapper) {
        this.queryInvoker = queryInvoker;
        this.invocationInputFactory = invocationInputFactory;
        this.graphQLObjectMapper = graphQLObjectMapper;
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {

        final WsSessionSubscriptions subscriptions = new WsSessionSubscriptions();
        final HandshakeRequest request = (HandshakeRequest) session.getUserProperties().get(HANDSHAKE_REQUEST_KEY);
        final SubscriptionProtocolHandler subscriptionProtocolHandler = (SubscriptionProtocolHandler) session.getUserProperties().get(PROTOCOL_HANDLER_REQUEST_KEY);

        sessionSubscriptionCache.put(session, subscriptions);

        // This *cannot* be a lambda because of the way undertow checks the class...
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String text) {
                try {
                    subscriptionProtocolHandler.onMessage(request, session, text);
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
        WsSessionSubscriptions subscriptions = sessionSubscriptionCache.remove(session);
        if(subscriptions != null) {
            subscriptions.close();
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        log.error("Error in websocket session: {}", session.getId(), thr);
        closeUnexpectedly(session, thr);
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

        List<String> accept = request.getHeaders().get(HandshakeResponse.SEC_WEBSOCKET_ACCEPT);
        if(accept == null) {
            accept = Collections.emptyList();
        }

        SubscriptionProtocolFactory subscriptionProtocolFactory = getSubscriptionProtocolFactory(accept);
        sec.getUserProperties().put(PROTOCOL_HANDLER_REQUEST_KEY, subscriptionProtocolFactory.createHandler(invocationInputFactory, queryInvoker, graphQLObjectMapper));

        if(request.getHeaders().get(HandshakeResponse.SEC_WEBSOCKET_ACCEPT) != null) {
            response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, allSubscriptionProtocols);
        }
        response.getHeaders().put(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL, Collections.singletonList(subscriptionProtocolFactory.getProtocol()));
    }

    private static SubscriptionProtocolFactory getSubscriptionProtocolFactory(List<String> accept) {
        for(String protocol: accept) {
            for(SubscriptionProtocolFactory subscriptionProtocolFactory: subscriptionProtocolFactories) {
                if(subscriptionProtocolFactory.getProtocol().equals(protocol)) {
                    return subscriptionProtocolFactory;
                }
            }
        }

        return fallbackSubscriptionProtocolFactory;
    }
}
