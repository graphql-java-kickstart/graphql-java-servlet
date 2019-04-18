package graphql.servlet.internal;

/**
 * @author Andrew Potter
 */
public class FallbackSubscriptionProtocolFactory extends SubscriptionProtocolFactory {
    private final SubscriptionHandlerInput subscriptionHandlerInput;

    public FallbackSubscriptionProtocolFactory(SubscriptionHandlerInput subscriptionHandlerInput) {
        super("");
        this.subscriptionHandlerInput = subscriptionHandlerInput;
    }

    @Override
    public SubscriptionProtocolHandler createHandler() {
        return new FallbackSubscriptionProtocolHandler(subscriptionHandlerInput);
    }
}
