package graphql.kickstart.execution.instrumentation;

import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.kickstart.execution.config.InstrumentationProvider;

public class NoOpInstrumentationProvider implements InstrumentationProvider {

  @Override
  public Instrumentation getInstrumentation() {
    return SimplePerformantInstrumentation.INSTANCE;
  }
}
