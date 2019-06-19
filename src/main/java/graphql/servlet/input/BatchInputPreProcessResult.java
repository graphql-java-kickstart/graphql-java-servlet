package graphql.servlet.input;

public class BatchInputPreProcessResult {

    private final GraphQLBatchedInvocationInput batchedInvocationInput;

    private final int statusCode;

    private final boolean executable;

    private final String messsage;

    public BatchInputPreProcessResult(GraphQLBatchedInvocationInput graphQLBatchedInvocationInput) {
        this.batchedInvocationInput = graphQLBatchedInvocationInput;
        this.executable = true;
        this.statusCode = 200;
        this.messsage = null;
   }

   public BatchInputPreProcessResult(int statusCode, String messsage) {
        this.batchedInvocationInput = null;
        this.executable = false;
        this.statusCode = statusCode;
        this.messsage = messsage;
   }

   public boolean isExecutable() {
        return executable;
   }

   public GraphQLBatchedInvocationInput getBatchedInvocationInput() {
        return batchedInvocationInput;
   }

   public String getStatusMessage() {
        return messsage;
   }

   public int getStatusCode() {
        return statusCode;
   }
}
