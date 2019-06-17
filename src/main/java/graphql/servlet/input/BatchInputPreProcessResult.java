package graphql.servlet.input;

public class BatchInputPreProcessResult {

    private final GraphQLBatchedInvocationInput batchedInvocationInput;

    private final boolean executable;

    public BatchInputPreProcessResult(GraphQLBatchedInvocationInput graphQLBatchedInvocationInput, boolean executable) {
        this.batchedInvocationInput = graphQLBatchedInvocationInput;
        this.executable = executable;
   }

   public boolean isExecutable() {
        return executable;
   }

   public GraphQLBatchedInvocationInput getBatchedInvocationInput() {
        return batchedInvocationInput;
   }
}
