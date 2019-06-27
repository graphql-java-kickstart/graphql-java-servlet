package graphql.servlet.context;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

/**
 * An object for the DefaultGraphQLContextBuilder to return. Can be extended to include more context.
 */
public class DefaultGraphQLContext implements GraphQLContext{

    private final Subject subject;

    private final DataLoaderRegistry dataLoaderRegistry;

    public DefaultGraphQLContext(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
        this.dataLoaderRegistry = dataLoaderRegistry;
        this.subject = subject;
    }

    public DefaultGraphQLContext() {
        this(null, null);
    }

    @Override
    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
    }

    @Override
    public Optional<DataLoaderRegistry> getDataLoaderRegistry() {
        return Optional.ofNullable(dataLoaderRegistry);
    }
}
