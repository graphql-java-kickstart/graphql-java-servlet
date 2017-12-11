package graphql.servlet.internal;

import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

/**
 * @author Andrew Potter
 */
public class ApolloSubscriptionProtocolFactory extends SubscriptionProtocolFactory {
    public ApolloSubscriptionProtocolFactory() {
        super("graphql-ws");
    }

    @Override
    public SubscriptionProtocolHandler createHandler(GraphQLInvocationInputFactory invocationInputFactory, GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper) {
        return new ApolloSubscriptionProtocolHandler();
    }
}
