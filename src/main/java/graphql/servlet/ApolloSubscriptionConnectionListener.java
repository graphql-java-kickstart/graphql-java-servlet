package graphql.servlet;

import java.time.Duration;
import java.util.Optional;

public interface ApolloSubscriptionConnectionListener extends SubscriptionConnectionListener {

    long KEEP_ALIVE_INTERVAL_SEC = 15;

    String CONNECT_RESULT_KEY = "CONNECT_RESULT";

    default boolean isKeepAliveEnabled() {
        return true;
    }

    default Optional<Object> onConnect(Object payload) throws SubscriptionException {
        return Optional.empty();
    }

    default Duration getKeepAliveInterval() {
        return Duration.ofSeconds(KEEP_ALIVE_INTERVAL_SEC);
    }

    static ApolloSubscriptionConnectionListener createWithKeepAliveDisabled() {
        return new ApolloSubscriptionConnectionListener() {
            @Override
            public boolean isKeepAliveEnabled() {
                return false;
            }
        };
    }

    static ApolloSubscriptionConnectionListener createWithKeepAliveInterval(Duration interval) {
        return new ApolloSubscriptionConnectionListener() {
            @Override
            public Duration getKeepAliveInterval() {
                return interval;
            }
        };
    }

}
