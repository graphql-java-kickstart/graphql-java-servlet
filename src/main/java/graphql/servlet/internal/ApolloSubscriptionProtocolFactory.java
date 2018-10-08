package graphql.servlet.internal;

/**
 * @author Andrew Potter
 */
public class ApolloSubscriptionProtocolFactory extends SubscriptionProtocolFactory {
    private final SubscriptionHandlerInput subscriptionHandlerInput;
    private final SubscriptionSender subscriptionSender;
    private final ApolloSubscriptionKeepAliveRunner keepAliveRunner;

    public ApolloSubscriptionProtocolFactory(SubscriptionHandlerInput subscriptionHandlerInput) {
        super("graphql-ws");
        this.subscriptionHandlerInput = subscriptionHandlerInput;
        subscriptionSender =
            new SubscriptionSender(subscriptionHandlerInput.getGraphQLObjectMapper().getJacksonMapper());
        keepAliveRunner = new ApolloSubscriptionKeepAliveRunner(subscriptionSender);
    }

    @Override
    public SubscriptionProtocolHandler createHandler() {
        return new ApolloSubscriptionProtocolHandler(subscriptionHandlerInput, subscriptionSender, keepAliveRunner);
    }
}
