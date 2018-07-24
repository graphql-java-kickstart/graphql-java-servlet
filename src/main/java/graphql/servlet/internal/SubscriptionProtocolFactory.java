package graphql.servlet.internal;

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

    public abstract SubscriptionProtocolHandler createHandler(SubscriptionHandlerInput subscriptionHandlerInput);
}
