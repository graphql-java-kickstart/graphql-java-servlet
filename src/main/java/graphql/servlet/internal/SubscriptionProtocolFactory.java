package graphql.servlet.internal;

import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

/**
 * @author Andrew Potter
 */
public abstract class SubscriptionProtocolFactory {
    private final String protocol;

    public SubscriptionProtocolFactory(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public abstract SubscriptionProtocolHandler createHandler(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper);
}
