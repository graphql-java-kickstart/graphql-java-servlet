package graphql.servlet;

import java.util.Optional;

public interface ApolloSubscriptionConnectionListener extends SubscriptionConnectionListener {

    String CONNECT_RESULT_KEY = "CONNECT_RESULT";

    default boolean isKeepAliveEnabled() {
        return true;
    }

    default Optional<Object> onConnect(Object payload) throws SubscriptionException {
        return Optional.empty();
    }

}
