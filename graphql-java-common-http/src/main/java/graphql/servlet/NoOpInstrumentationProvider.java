package graphql.servlet;

import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;

public class NoOpInstrumentationProvider implements InstrumentationProvider {

    @Override
    public Instrumentation getInstrumentation() {
        return SimpleInstrumentation.INSTANCE;
    }
}
