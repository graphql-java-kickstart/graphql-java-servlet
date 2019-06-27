package graphql.servlet.input;

import java.util.List;

/**
 * Interface representing a batched input.
 */
public interface GraphQLBatchedInvocationInput {

    /**
     * @return each individual input in the batch, configured with a context.
     */
    List<GraphQLSingleInvocationInput> getExecutionInputs();
}
