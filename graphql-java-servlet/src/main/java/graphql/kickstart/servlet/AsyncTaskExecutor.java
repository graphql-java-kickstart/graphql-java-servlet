package graphql.kickstart.servlet;

import java.util.concurrent.Executor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class AsyncTaskExecutor implements Executor {

  private final Executor executor;
  private final AsyncTaskDecorator taskDecorator;

  @Override
  public void execute(@NonNull Runnable command) {
    if (taskDecorator != null) {
      Runnable decorated = taskDecorator.decorate(command);
      executor.execute(decorated);
    } else {
      executor.execute(command);
    }
  }
}
