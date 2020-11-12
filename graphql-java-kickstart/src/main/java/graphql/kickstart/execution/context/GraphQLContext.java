package graphql.kickstart.execution.context;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Locale;
import java.util.Optional;

/**
 * Represents the context required by the servlet to execute a GraphQL request.
 */
public interface GraphQLContext {

    /**
     * @return the subject to execute the query as.
     */
    Optional<Subject> getSubject();

    /**
     * @return the Dataloader registry to use for the execution.
     */
    Optional<DataLoaderRegistry> getDataLoaderRegistry();

    /**
     * @return the locale to use for the execution
     */
    Optional<Locale> getLocale();
}
