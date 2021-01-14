package graphql.kickstart.execution.config;

import graphql.execution.instrumentation.Instrumentation;

public interface InstrumentationProvider {

  Instrumentation getInstrumentation();
}
