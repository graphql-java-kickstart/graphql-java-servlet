package graphql.servlet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.servlet.config.ObjectMapperConfigurer;
import graphql.servlet.config.ObjectMapperProvider;
import graphql.servlet.core.DefaultObjectMapperConfigurer;

public class ConfiguringObjectMapperProvider implements ObjectMapperProvider {

  private final ObjectMapper objectMapperTemplate;

  private final ObjectMapperConfigurer objectMapperConfigurer;

  public ConfiguringObjectMapperProvider(ObjectMapper objectMapperTemplate, ObjectMapperConfigurer objectMapperConfigurer) {
    this.objectMapperTemplate = objectMapperTemplate == null ? new ObjectMapper() : objectMapperTemplate;
    this.objectMapperConfigurer = objectMapperConfigurer == null ? new DefaultObjectMapperConfigurer() : objectMapperConfigurer;
  }

  public ConfiguringObjectMapperProvider(ObjectMapper objectMapperTemplate) {
      this(objectMapperTemplate, null);
  }

  public ConfiguringObjectMapperProvider(ObjectMapperConfigurer objectMapperConfigurer) {
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
