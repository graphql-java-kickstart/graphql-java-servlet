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
    public SubscriptionProtocolHandler createHandler(SubscriptionHandlerInput subscriptionHandlerInput) {
        return new FallbackSubscriptionProtocolHandler(subscriptionHandlerInput);
    }
}
