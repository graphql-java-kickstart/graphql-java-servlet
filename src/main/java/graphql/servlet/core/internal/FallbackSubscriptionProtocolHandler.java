package graphql.servlet.core.internal;

import graphql.servlet.GraphQLSingleInvocationInput;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.UUID;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolHandler extends SubscriptionProtocolHandler {

    private final SubscriptionHandlerInput input;
    private final SubscriptionSender sender;

    public FallbackSubscriptionProtocolHandler(SubscriptionHandlerInput subscriptionHandlerInput) {
        this.input = subscriptionHandlerInput;
        sender = new SubscriptionSender(subscriptionHandlerInput.getGraphQLObjectMapper().getJacksonMapper());
    }

    @Override
    public void onMessage(HandshakeRequest request, Session session, WsSessionSubscriptions subscriptions, String text) throws Exception {
        GraphQLSingleInvocationInput graphQLSingleInvocationInput = createInvocationInput(session, text);
        subscribe(
            session,
            input.getQueryInvoker().query(graphQLSingleInvocationInput),
            subscriptions,
            UUID.randomUUID().toString()
        );
    }

    private GraphQLSingleInvocationInput createInvocationInput(Session session, String text) throws IOException {
        GraphQLRequest graphQLRequest = input.getGraphQLObjectMapper().readGraphQLRequest(text);
        HandshakeRequest handshakeRequest = (HandshakeRequest) session.getUserProperties()
                .get(HandshakeRequest.class.getName());

        return input.getInvocationInputFactory().create(graphQLRequest, session, handshakeRequest);
    }

    @Override
    protected void sendDataMessage(Session session, String id, Object payload) {
        sender.send(session, payload);
    }

    @Override
    protected void sendErrorMessage(Session session, String id) {

    }

    @Override
    protected void sendCompleteMessage(Session session, String id) {

    }
}
