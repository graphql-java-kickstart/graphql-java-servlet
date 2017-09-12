package graphql.servlet;

import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.NoOpInstrumentation;

public class NoOpInstrumentationProvider implements InstrumentationProvider {

    @Override
    public Instrumentation getInstrumentation() {
        return NoOpInstrumentation.INSTANCE;
    }
}
