package graphql.kickstart.execution.subscriptions.apollo;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.subscriptions.SubscriptionSession;
import graphql.kickstart.execution.subscriptions.apollo.OperationMessage.Type;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ApolloSubscriptionConsumer implements Consumer<String> {

  private final SubscriptionSession session;
  private final GraphQLObjectMapper objectMapper;
  private final ApolloCommandProvider commandProvider;

  @Override
  public void accept(String request) {
    try {
      OperationMessage message =
          objectMapper.getJacksonMapper().readValue(request, OperationMessage.class);
      SubscriptionCommand command = commandProvider.getByType(message.getType());
      command.apply(session, message);
    } catch (JsonProcessingException e) {
      log.error("Cannot read subscription command '{}'", request, e);
      session.sendMessage(new OperationMessage(Type.GQL_CONNECTION_ERROR, null, e.getMessage()));
    }
  }
}
