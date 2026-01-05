package graphql.kickstart.servlet;

import graphql.incremental.DelayedIncrementalPartialResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class DelayedIncrementalPartialResultSubscriber implements Subscriber<DelayedIncrementalPartialResult> {

  private final AtomicReference<Subscription> subscriptionRef;
  private final AsyncContext asyncContext;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final CountDownLatch completedLatch = new CountDownLatch(1);

  DelayedIncrementalPartialResultSubscriber(
      AtomicReference<Subscription> subscriptionRef,
      AsyncContext asyncContext,
      GraphQLObjectMapper graphQLObjectMapper) {
    this.subscriptionRef = subscriptionRef;
    this.asyncContext = asyncContext;
    this.graphQLObjectMapper = graphQLObjectMapper;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscriptionRef.set(subscription);
    subscriptionRef.get().request(1);
  }

  @Override
  public void onNext(DelayedIncrementalPartialResult delayedIncrementalPartialResult) {
    try {
      ServletResponse response = asyncContext.getResponse();
      ServletOutputStream outputStream = response.getOutputStream();
      outputStream.write(HttpRequestHandler.MULTIPART_BOUNDARY.getBytes(StandardCharsets.UTF_8));
      outputStream.write(HttpRequestHandler.MULTIPART_CONTENT_TYPE.getBytes(
          StandardCharsets.UTF_8));
      byte[] contentBytes = graphQLObjectMapper.serializeDelayedIncrementalResultsAsBytes(delayedIncrementalPartialResult);
      outputStream.write(contentBytes);
      outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
      if (!delayedIncrementalPartialResult.hasNext()) {
        outputStream.write(HttpRequestHandler.MULTIPART_BOUNDARY.getBytes(StandardCharsets.UTF_8));
      }
      outputStream.flush();
      subscriptionRef.get().request(1);
    } catch (IOException ignored) {
      // ignore
    }
  }

  @Override
  public void onError(Throwable t) {
    asyncContext.complete();
    completedLatch.countDown();
  }

  @Override
  public void onComplete() {
    asyncContext.complete();
    completedLatch.countDown();
  }

  void await() throws InterruptedException {
    completedLatch.await();
  }
}
