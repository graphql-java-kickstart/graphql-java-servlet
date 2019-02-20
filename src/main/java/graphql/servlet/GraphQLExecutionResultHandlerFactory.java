package graphql.servlet;

import java.io.Writer;

/**
 * Interface to allow customization of how batched queries are handled.
 */
public interface GraphQLExecutionResultHandlerFactory {
    /**
     * Produces an ExecutionResultHandler instance for a specific response. Can maintain state across each request within a batch.
     * @param respWriter to send the response back
     * @param graphQLObjectMapper to serialize results.
     * @return a handler instance
     */
    ExecutionResultHandler getBatchHandler(Writer respWriter, GraphQLObjectMapper graphQLObjectMapper);
}
