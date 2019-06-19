package graphql.servlet.instrumentation;

import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.servlet.config.InstrumentationProvider;

public class NoOpInstrumentationProvider implements InstrumentationProvider {

    @Override
    public Instrumentation getInstrumentation() {
        return SimpleInstrumentation.INSTANCE;
    }
}
