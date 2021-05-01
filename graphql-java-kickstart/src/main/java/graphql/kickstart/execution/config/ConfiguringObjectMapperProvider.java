package graphql.kickstart.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.kickstart.execution.error.DefaultGraphQLServletObjectMapperConfigurer;

public class ConfiguringObjectMapperProvider implements ObjectMapperProvider {

  private final ObjectMapper objectMapperTemplate;

  private final GraphQLServletObjectMapperConfigurer objectMapperConfigurer;

  public ConfiguringObjectMapperProvider(
      ObjectMapper objectMapperTemplate,
      GraphQLServletObjectMapperConfigurer objectMapperConfigurer) {
    this.objectMapperTemplate =
        objectMapperTemplate == null ? new ObjectMapper() : objectMapperTemplate;
    this.objectMapperConfigurer =
        objectMapperConfigurer == null
            ? new DefaultGraphQLServletObjectMapperConfigurer()
            : objectMapperConfigurer;
  }

  public ConfiguringObjectMapperProvider(ObjectMapper objectMapperTemplate) {
    this(objectMapperTemplate, null);
  }

  public ConfiguringObjectMapperProvider(
      GraphQLServletObjectMapperConfigurer objectMapperConfigurer) {
    this(null, objectMapperConfigurer);
  }

  public ConfiguringObjectMapperProvider() {
    this(null, null);
  }

  @Override
  public ObjectMapper provide() {
    ObjectMapper mapper = this.objectMapperTemplate.copy();
    this.objectMapperConfigurer.configure(mapper);
    return mapper;
  }
}
