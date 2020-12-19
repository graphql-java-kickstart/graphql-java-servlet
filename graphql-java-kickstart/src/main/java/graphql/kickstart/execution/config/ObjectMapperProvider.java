package graphql.kickstart.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperProvider {

  ObjectMapper provide();
}
