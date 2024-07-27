package graphql.kickstart.execution.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import graphql.kickstart.execution.config.GraphQLServletObjectMapperConfigurer;

/** @author Andrew Potter */
public class DefaultGraphQLServletObjectMapperConfigurer
    implements GraphQLServletObjectMapperConfigurer {

  @Override
  public void configure(ObjectMapper mapper) {
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
  }
}
