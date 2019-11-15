package graphql.servlet;

import static graphql.servlet.HttpRequestHandler.APPLICATION_EVENT_STREAM_UTF8;
import static graphql.servlet.HttpRequestHandler.APPLICATION_JSON_UTF8;
import static graphql.servlet.HttpRequestHandler.STATUS_OK;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.servlet.core.GraphQLObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
class SingleAsynchronousQueryResponseWriter implements QueryResponseWriter {

  @Getter
  private final DecoratedExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final long subscriptionTimeout;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) {
    Objects.requireNonNull(request, "Http servlet request cannot be null");
    response.setContentType(APPLICATION_EVENT_STREAM_UTF8);
    response.setStatus(STATUS_OK);

    boolean isInAsyncThread = request.isAsyncStarted();
    AsyncContext asyncContext = isInAsyncThread ? request.getAsyncContext() : request.startAsync(request, response);
    asyncContext.setTimeout(subscriptionTimeout);
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    asyncContext.addListener(new SubscriptionAsyncListener(subscriptionRef));
    ExecutionResultSubscriber subscriber = new ExecutionResultSubscriber(subscriptionRef, asyncContext,
        graphQLObjectMapper);
    List<Publisher<ExecutionResult>> publishers = new ArrayList<>();
    if (result.getData() instanceof Publisher) {
      publishers.add(result.getData());
    } else {
      publishers.add(new StaticDataPublisher<>(result));
      final Publisher<ExecutionResult> deferredResultsPublisher = (Publisher<ExecutionResult>) result.getExtensions()
          .get(GraphQL.DEFERRED_RESULTS);
      publishers.add(deferredResultsPublisher);
    }
    publishers.forEach(it -> it.subscribe(subscriber));

    if (isInAsyncThread) {
      // We need to delay the completion of async context until after the subscription has terminated, otherwise the AsyncContext is prematurely closed.
      try {
        subscriber.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

}
