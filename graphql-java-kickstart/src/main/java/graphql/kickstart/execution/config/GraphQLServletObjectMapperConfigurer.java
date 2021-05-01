package graphql.kickstart.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;

/** @author Andrew Potter */
public interface GraphQLServletObjectMapperConfigurer {

  void configure(ObjectMapper mapper);
}
