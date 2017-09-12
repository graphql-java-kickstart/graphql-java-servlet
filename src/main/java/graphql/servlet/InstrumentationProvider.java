package graphql.servlet;

import graphql.execution.instrumentation.Instrumentation;

public interface InstrumentationProvider {
    Instrumentation getInstrumentation();
}
