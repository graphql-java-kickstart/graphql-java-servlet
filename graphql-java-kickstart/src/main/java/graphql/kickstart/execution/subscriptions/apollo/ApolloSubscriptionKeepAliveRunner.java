package graphql.kickstart.execution.subscriptions.apollo;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ApolloSubscriptionKeepAliveRunner {

  private static final int EXECUTOR_POOL_SIZE = 10;

  private final ScheduledExecutorService executor;
  private final OperationMessage keepAliveMessage;
  private final Map<SubscriptionSession, Future<?>> futures;
  private final long keepAliveIntervalSeconds;

  ApolloSubscriptionKeepAliveRunner(Duration keepAliveInterval) {
    this.keepAliveMessage = OperationMessage.newKeepAliveMessage();
    this.executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
    this.futures = new ConcurrentHashMap<>();
    this.keepAliveIntervalSeconds = keepAliveInterval.getSeconds();
  }

  void keepAlive(SubscriptionSession session) {
    futures.computeIfAbsent(session, this::startKeepAlive);
  }

  private ScheduledFuture<?> startKeepAlive(SubscriptionSession session) {
    return executor.scheduleAtFixedRate(
        () -> {
          try {
            if (session.isOpen()) {
              session.sendMessage(keepAliveMessage);
            } else {
              log.debug("Session {} appears to be closed. Aborting keep alive", session.getId());
              abort(session);
            }
          } catch (Exception t) {
            log.error(
                "Cannot send keep alive message to session {}. Aborting keep alive",
                session.getId(),
                t);
            abort(session);
          }
        },
        0,
        keepAliveIntervalSeconds,
        TimeUnit.SECONDS);
  }

  void abort(SubscriptionSession session) {
    Future<?> future = futures.remove(session);
    if (future != null) {
      future.cancel(true);
    }
  }

  void shutdown() {
    this.executor.shutdown();
  }
}
