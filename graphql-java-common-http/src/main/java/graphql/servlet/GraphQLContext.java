package graphql.servlet;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

public interface GraphQLContext {

    Optional<Subject> getSubject();

    void setSubject(Subject subject);

    DataLoaderRegistry getDataLoaderRegistry();

    void setDataLoaderRegistry(DataLoaderRegistry dataLoaderRegistry);

}
