package graphql.kickstart.servlet;

import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

interface AsyncTimeoutListener extends AsyncListener {

  default void onComplete(AsyncEvent event) throws IOException {}

  default void onError(AsyncEvent event) throws IOException {}

  default void onStartAsync(AsyncEvent event) throws IOException {}
}
