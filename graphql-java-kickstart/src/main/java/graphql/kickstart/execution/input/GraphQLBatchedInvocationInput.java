package graphql.kickstart.execution.input;

import graphql.kickstart.execution.context.ContextSetting;
import graphql.kickstart.execution.input.GraphQLInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
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
