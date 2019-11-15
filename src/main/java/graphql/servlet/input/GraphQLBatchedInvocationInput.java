package graphql.servlet.input;

import graphql.servlet.context.ContextSetting;
import java.util.List;

/**
 * Interface representing a batched input.
 */
public interface GraphQLBatchedInvocationInput extends GraphQLInvocationInput {

    /**
     * @return each individual input in the batch, configured with a context.
     */
    List<GraphQLSingleInvocationInput> getExecutionInputs();

    ContextSetting getContextSetting();

}
