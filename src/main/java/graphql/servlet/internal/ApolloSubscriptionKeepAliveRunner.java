package graphql.servlet.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ApolloSubscriptionKeepAliveRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ApolloSubscriptionKeepAliveRunner.class);

    private static final long KEEP_ALIVE_INTERVAL_SEC = 15;
    private static final int EXECUTOR_POOL_SIZE = 10;

    private ScheduledExecutorService executor;
    private SubscriptionSender sender;
    private ApolloSubscriptionProtocolHandler.OperationMessage keepAliveMessage;
    private Map<Session, Future<?>> futures;

    ApolloSubscriptionKeepAliveRunner(SubscriptionSender sender) {
        this.sender = sender;
        this.keepAliveMessage = ApolloSubscriptionProtocolHandler.OperationMessage.newKeepAliveMessage();
        this.executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
        this.futures = new ConcurrentHashMap<>();
    }

    void keepAlive(Session session) {
        if (!futures.containsKey(session)) {
            Future<?> future = executor.scheduleAtFixedRate(() -> {
                try {
                    if (session.isOpen()) {
                        sender.send(session, keepAliveMessage);
                    } else {
                        LOG.warn("Session appears to be closed. Aborting keep alive");
                        abort(session);
                    }
                } catch (Throwable t) {
                    LOG.error("Cannot send keep alive message. Aborting keep alive", t);
                    abort(session);
                }
            }, 0, KEEP_ALIVE_INTERVAL_SEC, TimeUnit.SECONDS);
            futures.put(session, future);
        }
    }

    void abort(Session session) {
        Future<?> future = futures.remove(session);
        if (future != null) {
            future.cancel(true);
        }
    }

}
