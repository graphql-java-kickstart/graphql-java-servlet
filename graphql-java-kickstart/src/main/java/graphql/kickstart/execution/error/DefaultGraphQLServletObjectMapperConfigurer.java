package graphql.kickstart.execution.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import graphql.kickstart.execution.config.GraphQLServletObjectMapperConfigurer;

/** @author Andrew Potter */
public class DefaultGraphQLServletObjectMapperConfigurer
    implements GraphQLServletObjectMapperConfigurer {

  @Override
  public void configure(ObjectMapper mapper) {
    // default configuration for GraphQL Java Servlet
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.registerModule(new Jdk8Module());
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
  }
}
