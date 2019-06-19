package graphql.servlet.context;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

public interface GraphQLContext {

    Optional<Subject> getSubject();

    Optional<DataLoaderRegistry> getDataLoaderRegistry();
}
