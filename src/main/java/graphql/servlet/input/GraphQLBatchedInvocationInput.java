package graphql.servlet.input;

import java.util.List;

public interface GraphQLBatchedInvocationInput {

    List<GraphQLSingleInvocationInput> getExecutionInputs();
}
