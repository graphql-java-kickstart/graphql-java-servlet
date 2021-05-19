package graphql.kickstart.servlet;

public interface AsyncTaskDecorator {

  Runnable decorate(Runnable runnable);

}
