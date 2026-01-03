package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.incremental.DelayedIncrementalPartialResult;
import graphql.incremental.IncrementalExecutionResult;
import graphql.incremental.IncrementalPayload;
import graphql.kickstart.execution.GraphQLObjectMapper;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

@RequiredArgsConstructor
class SingleIncrementalQueryResponseWriter implements QueryResponseWriter {

  @Getter private final IncrementalExecutionResult result;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final long subscriptionTimeout;

  @Override
  public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Objects.requireNonNull(request, "Http servlet request cannot be null");
    response.setContentType(HttpRequestHandler.MULTIPART_MIXED);
    response.setStatus(HttpRequestHandler.STATUS_OK);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    // Write the initial data
    ServletOutputStream outputStream = response.getOutputStream();
    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    outputStream.write(HttpRequestHandler.MULTIPART_BOUNDARY.getBytes(StandardCharsets.UTF_8));
    outputStream.write(HttpRequestHandler.MULTIPART_CONTENT_TYPE.getBytes(
        StandardCharsets.UTF_8));
    byte[] contentBytes = graphQLObjectMapper.serializeResultAsBytes(result);
    outputStream.write(contentBytes);
    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    outputStream.flush();

    // If no more data is expected, we can just complete the response here
    boolean isInAsyncThread = request.isAsyncStarted();
    AsyncContext asyncContext =
        isInAsyncThread ? request.getAsyncContext() : request.startAsync(request, response);
    if (!result.hasNext()) {
      asyncContext.complete();
      return;
    }

    // Now handle any delayed incremental payloads
    asyncContext.setTimeout(subscriptionTimeout);
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    asyncContext.addListener(new SubscriptionAsyncListener(subscriptionRef));
    DelayedIncrementalPartialResultSubscriber subscriber =
        new DelayedIncrementalPartialResultSubscriber(subscriptionRef, asyncContext, graphQLObjectMapper);
    var publisher = result.getIncrementalItemPublisher();
    publisher.subscribe(subscriber);

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
