package graphql.servlet.config;

import graphql.execution.instrumentation.Instrumentation;

public interface InstrumentationProvider {
    Instrumentation getInstrumentation();
}
