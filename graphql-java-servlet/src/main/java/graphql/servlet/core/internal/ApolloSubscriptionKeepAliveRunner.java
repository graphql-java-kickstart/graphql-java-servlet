package graphql.servlet.core.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class ApolloSubscriptionKeepAliveRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ApolloSubscriptionKeepAliveRunner.class);

    private static final int EXECUTOR_POOL_SIZE = 10;

    private final ScheduledExecutorService executor;
    private final SubscriptionSender sender;
    private final ApolloSubscriptionProtocolHandler.OperationMessage keepAliveMessage;
    private final Map<Session, Future<?>> futures;
    private final long keepAliveIntervalSeconds;

    ApolloSubscriptionKeepAliveRunner(SubscriptionSender sender, Duration keepAliveInterval) {
        this.sender = Objects.requireNonNull(sender);
        this.keepAliveMessage = ApolloSubscriptionProtocolHandler.OperationMessage.newKeepAliveMessage();
        this.executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
        this.futures = new ConcurrentHashMap<>();
        this.keepAliveIntervalSeconds = keepAliveInterval.getSeconds();
    }

    void keepAlive(Session session) {
        futures.computeIfAbsent(session, this::startKeepAlive);
    }

    private ScheduledFuture<?> startKeepAlive(Session session) {
        return executor.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    sender.send(session, keepAliveMessage);
                } else {
                    LOG.debug("Session {} appears to be closed. Aborting keep alive", session.getId());
                    abort(session);
                }
            } catch (Throwable t) {
                LOG.error("Cannot send keep alive message to session {}. Aborting keep alive", session.getId(), t);
                abort(session);
            }
        }, 0, keepAliveIntervalSeconds, TimeUnit.SECONDS);
    }

    void abort(Session session) {
        Future<?> future = futures.remove(session);
        if (future != null) {
            future.cancel(true);
        }
    }

}
