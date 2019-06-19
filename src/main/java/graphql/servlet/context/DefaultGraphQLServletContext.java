package graphql.servlet.context;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

public class DefaultGraphQLServletContext implements GraphQLContext{

    private final Subject subject;

    private final DataLoaderRegistry dataLoaderRegistry;

    public DefaultGraphQLServletContext(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
        this.dataLoaderRegistry = dataLoaderRegistry;
        this.subject = subject;
    }

    public DefaultGraphQLServletContext() {
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
