package graphql.kickstart.execution.context;

public class ContextSettingNotConfiguredException extends RuntimeException {

  ContextSettingNotConfiguredException() {
    super("Unconfigured context setting type");
  }
}
