package graphql.servlet.internal;

import graphql.servlet.ApolloSubscriptionConnectionListener;

/**
 * @author Andrew Potter
 */
public class ApolloSubscriptionProtocolFactory extends SubscriptionProtocolFactory {
    private final SubscriptionHandlerInput subscriptionHandlerInput;
    private final SubscriptionSender subscriptionSender;
    private final ApolloSubscriptionKeepAliveRunner keepAliveRunner;
    private final ApolloSubscriptionConnectionListener connectionListener;

    public ApolloSubscriptionProtocolFactory(SubscriptionHandlerInput subscriptionHandlerInput) {
        super("graphql-ws");
        this.subscriptionHandlerInput = subscriptionHandlerInput;
        this.connectionListener = subscriptionHandlerInput.getSubscriptionConnectionListener()
                .filter(ApolloSubscriptionConnectionListener.class::isInstance)
                .map(ApolloSubscriptionConnectionListener.class::cast)
                .orElse(new ApolloSubscriptionConnectionListener() {});
        subscriptionSender =
            new SubscriptionSender(subscriptionHandlerInput.getGraphQLObjectMapper().getJacksonMapper());
        keepAliveRunner = new ApolloSubscriptionKeepAliveRunner(subscriptionSender, connectionListener.getKeepAliveInterval());
    }

    @Override
    public SubscriptionProtocolHandler createHandler() {
        return new ApolloSubscriptionProtocolHandler(subscriptionHandlerInput, connectionListener, subscriptionSender, keepAliveRunner);
    }
}
