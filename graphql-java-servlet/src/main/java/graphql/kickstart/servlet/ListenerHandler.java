package graphql.kickstart.servlet;

import static java.util.Collections.emptyList;

import graphql.kickstart.servlet.core.GraphQLServletListener;
import graphql.kickstart.servlet.core.GraphQLServletListener.RequestCallback;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ListenerHandler {

  private final List<RequestCallback> callbacks;
  private final HttpServletRequest request;
  private final HttpServletResponse response;

  static ListenerHandler start(
      HttpServletRequest request,
      HttpServletResponse response,
      List<GraphQLServletListener> listeners) {
    if (listeners != null) {
      return new ListenerHandler(
          runListeners(listeners, it -> it.onRequest(request, response)), request, response);
    }
    return new ListenerHandler(emptyList(), request, response);
  }

  private static <R> List<R> runListeners(
      List<GraphQLServletListener> listeners, Function<? super GraphQLServletListener, R> action) {
    return listeners.stream()
        .map(
            listener -> {
              try {
                return action.apply(listener);
              } catch (Exception t) {
                log.error("Error running listener: {}", listener, t);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  void runCallbacks(Consumer<RequestCallback> action) {
    callbacks.forEach(
        callback -> {
          try {
            action.accept(callback);
          } catch (Exception t) {
            log.error("Error running callback: {}", callback, t);
          }
        });
  }

  void onParseError(Throwable throwable) {
    runCallbacks(it -> it.onParseError(request, response, throwable));
  }

  void beforeFlush() {
    runCallbacks(it -> it.beforeFlush(request, response));
  }

  void onSuccess() {
    runCallbacks(it -> it.onSuccess(request, response));
  }

  void onError(Throwable throwable) {
    runCallbacks(it -> it.onError(request, response, throwable));
  }

  void onFinally() {
    runCallbacks(it -> it.onFinally(request, response));
  }
}
