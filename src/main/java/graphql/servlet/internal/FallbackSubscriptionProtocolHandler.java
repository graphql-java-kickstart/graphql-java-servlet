package graphql.servlet.internal;

import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolHandler implements SubscriptionProtocolHandler {

    private final GraphQLQueryInvoker queryInvoker;
    private final GraphQLInvocationInputFactory invocationInputFactory;
    private final GraphQLObjectMapper graphQLObjectMapper;

    public FallbackSubscriptionProtocolHandler(GraphQLQueryInvoker queryInvoker, GraphQLInvocationInputFactory invocationInputFactory, GraphQLObjectMapper graphQLObjectMapper) {
        this.queryInvoker = queryInvoker;
        this.invocationInputFactory = invocationInputFactory;
        this.graphQLObjectMapper = graphQLObjectMapper;
    }

    @Override
    public void onMessage(HandshakeRequest request, Session session, String text) throws Exception {
        session.getBasicRemote().sendText(graphQLObjectMapper.serializeResultAsJson(
            queryInvoker.query(invocationInputFactory.create(graphQLObjectMapper.readGraphQLRequest(text), request))
        ));
    }
}
