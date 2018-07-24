package graphql.servlet.internal;

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
