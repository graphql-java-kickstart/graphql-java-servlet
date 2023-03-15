package graphql.kickstart.servlet.subscriptions;

import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import java.util.function.Consumer;
import javax.websocket.Session;

public interface WebSocketSubscriptionProtocolFactory {

  Consumer<String> createConsumer(SubscriptionSession session);

  SubscriptionSession createSession(Session session);
}
