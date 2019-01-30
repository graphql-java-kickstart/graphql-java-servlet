package graphql.servlet;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;

import java.util.concurrent.CompletableFuture;

public class TestInstrumentation extends SimpleInstrumentation {
    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters) {
        ExecutionResultImpl.Builder builder = ExecutionResultImpl.newExecutionResult().from((ExecutionResultImpl) executionResult);
        GraphQLContext context = parameters.getContext();
        if (context.getHttpServletRequest().map(req -> req.getHeader("requestHeaderTest")).isPresent()) {
            builder.addExtension("requestHeaderTest", "true");
        }
        return CompletableFuture.completedFuture(builder.build());
    }

}
