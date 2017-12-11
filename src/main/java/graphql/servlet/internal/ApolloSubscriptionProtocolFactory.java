package graphql.servlet.internal;

/**
 * @author Andrew Potter
 */
public class ApolloSubscriptionProtocolFactory extends SubscriptionProtocolFactory {
    public ApolloSubscriptionProtocolFactory() {
        super("graphql-ws");
    }

    @Override
    public SubscriptionProtocolHandler createHandler() {
        return new ApolloSubscriptionProtocolHandler();
    }
}
