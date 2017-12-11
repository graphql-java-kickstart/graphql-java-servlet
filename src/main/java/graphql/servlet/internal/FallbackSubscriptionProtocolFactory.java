package graphql.servlet.internal;

import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolFactory extends SubscriptionProtocolFactory {
    public FallbackSubscriptionProtocolFactory() {
        super("");
    }

    @Override
    public SubscriptionProtocolHandler createHandler(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper) {
        return new FallbackSubscriptionProtocolHandler(queryInvoker, invocationInputFactory, graphQLObjectMapper);
    }
}
