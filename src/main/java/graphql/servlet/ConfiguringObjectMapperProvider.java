package graphql.servlet;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class ConfiguringObjectMapperProvider implements ObjectMapperProvider {

  private final ObjectMapperConfigurer objectMapperConfigurer;

  public ConfiguringObjectMapperProvider(ObjectMapperConfigurer objectMapperConfigurer) {
    this.objectMapperConfigurer = objectMapperConfigurer;
  }

  public ConfiguringObjectMapperProvider() {
    this.objectMapperConfigurer = new DefaultObjectMapperConfigurer();
  }

  @Override
  public ObjectMapper provide() {
    ObjectMapper mapper = new ObjectMapper().disable(
      SerializationFeature.FAIL_ON_EMPTY_BEANS).registerModule(new Jdk8Module());
    objectMapperConfigurer.configure(mapper);

    return mapper;
  }
}
