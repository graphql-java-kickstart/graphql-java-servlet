package graphql.servlet;

import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import java.util.Optional;

interface GraphQLContext {

    Optional<Subject> getSubject();

    Optional<DataLoaderRegistry> getDataLoaderRegistry();

}
