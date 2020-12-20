package graphql.kickstart.execution.context;

import java.util.Optional;
import javax.security.auth.Subject;
import lombok.NonNull;
import org.dataloader.DataLoaderRegistry;

/**
 * Represents the context required by the servlet to execute a GraphQL request.
 */
public interface GraphQLContext {

  /**
   * @return the subject to execute the query as.
   */
  Optional<Subject> getSubject();

  /**
   * @return the Dataloader registry to use for the execution. Must not return <code>null</code>
   */
  @NonNull DataLoaderRegistry getDataLoaderRegistry();

}
