package graphql.servlet;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Potter
 */
public class DefaultGraphQLErrorHandler implements GraphQLErrorHandler {

    public static final Logger log = LoggerFactory.getLogger(DefaultGraphQLErrorHandler.class);

    @Override
    public List<GraphQLError> processErrors(List<GraphQLError> errors) {
        final List<GraphQLError> clientErrors = filterGraphQLErrors(errors);
        if (clientErrors.size() < errors.size()) {

            // Some errors were filtered out to hide implementation - put a generic error in place.
            clientErrors.add(new GenericGraphQLError("Internal Server Error(s) while executing query"));

            errors.stream()
                .filter(error -> !isClientError(error))
                .forEach(error -> {
                    if(error instanceof Throwable) {
                        log.error("Error executing query!", (Throwable) error);
                    } else {
                        log.error("Error executing query ({}): {}", error.getClass().getSimpleName(), error.getMessage());
                    }
                });
        }

        return clientErrors;
    }

    protected List<GraphQLError> filterGraphQLErrors(List<GraphQLError> errors) {
        return errors.stream()
            .filter(this::isClientError)
            .collect(Collectors.toList());
    }

    protected boolean isClientError(GraphQLError error) {
        return !(error instanceof ExceptionWhileDataFetching || error instanceof Throwable);
    }
}
