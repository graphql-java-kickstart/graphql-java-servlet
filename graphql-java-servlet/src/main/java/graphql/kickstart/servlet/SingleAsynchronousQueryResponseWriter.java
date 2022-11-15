package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
class SingleAsynchronousQueryResponseWriter implements QueryResponseWriter {

  @Getter private final ExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final long subscriptionTimeout;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) {
    Objects.requireNonNull(request, "Http servlet request cannot be null");
    response.setContentType(HttpRequestHandler.APPLICATION_EVENT_STREAM_UTF8);
    response.setStatus(HttpRequestHandler.STATUS_OK);

    boolean isInAsyncThread = request.isAsyncStarted();
    AsyncContext asyncContext =
        isInAsyncThread ? request.getAsyncContext() : request.startAsync(request, response);
    asyncContext.setTimeout(subscriptionTimeout);
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    asyncContext.addListener(new SubscriptionAsyncListener(subscriptionRef));
    ExecutionResultSubscriber subscriber =
        new ExecutionResultSubscriber(subscriptionRef, asyncContext, graphQLObjectMapper);
    List<Publisher<ExecutionResult>> publishers = new ArrayList<>();
    if (result.getData() instanceof Publisher) {
      publishers.add(result.getData());
    } else {
      publishers.add(new StaticDataPublisher<>(result));
    }
    publishers.forEach(it -> it.subscribe(subscriber));

    if (isInAsyncThread) {
      // We need to delay the completion of async context until after the subscription has
      // terminated, otherwise the AsyncContext is prematurely closed.
      try {
        subscriber.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
