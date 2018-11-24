package graphql.servlet;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Objects;
import java.util.Optional;

public class DefaultGraphQLContext implements GraphQLContext {

    private Subject subject;
    private DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();

    public DefaultGraphQLContext() {

    }

    public DefaultGraphQLContext(Subject subject, DataLoaderRegistry dataLoaderRegistry) {
        this.subject = subject;
        if (dataLoaderRegistry != null) {
            this.dataLoaderRegistry = dataLoaderRegistry;
        }
    }

    @Override
    public Optional<Subject> getSubject() {
        return Optional.ofNullable(subject);
    }

    @Override
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    @Override
    public DataLoaderRegistry getDataLoaderRegistry() {
        return dataLoaderRegistry;
    }

    @Override
    public void setDataLoaderRegistry(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = Objects.requireNonNull(dataLoaderRegistry);
    }

}
