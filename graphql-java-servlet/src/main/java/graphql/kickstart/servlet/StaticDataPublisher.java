package graphql.kickstart.servlet;

import graphql.execution.reactive.SingleSubscriberPublisher;
import org.reactivestreams.Publisher;

class StaticDataPublisher<T> extends SingleSubscriberPublisher<T> implements Publisher<T> {

  StaticDataPublisher(T data) {
    super();
    offer(data);
    noMoreData();
  }
}
