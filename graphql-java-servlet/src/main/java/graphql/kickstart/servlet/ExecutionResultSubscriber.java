package graphql.kickstart.servlet;

import graphql.ExecutionResult;
import graphql.kickstart.execution.GraphQLObjectMapper;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.AsyncContext;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class ExecutionResultSubscriber implements Subscriber<ExecutionResult> {

  private final AtomicReference<Subscription> subscriptionRef;
  private final AsyncContext asyncContext;
  private final GraphQLObjectMapper graphQLObjectMapper;
  private final CountDownLatch completedLatch = new CountDownLatch(1);

  ExecutionResultSubscriber(
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
  public void onNext(ExecutionResult executionResult) {
    try {
      Writer writer = asyncContext.getResponse().getWriter();
      writer.write("data: ");
      writer.write(graphQLObjectMapper.serializeResultAsJson(executionResult));
      writer.write("\n\n");
      writer.flush();
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
